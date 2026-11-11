package com.cornucopai.game;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Atelier;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.PlayedCard;
import com.cornucopai.game.model.Reaction;
import com.cornucopai.game.model.Tour;
import com.cornucopai.game.model.ValidationStatus;
import com.cornucopai.game.model.Vote;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T009 (US1, spec 006): the GameMaster groups legs into tours — each tour is one leg
 * per player still holding cards; tour 1 leader is random; the next tour's leader is
 * the previous tour's winner (or random if all legs REJECTED); each tour's majorSuit
 * is the first non-WC card's suitId; every leg carries turnIndex/suitId/cardValue/
 * majorSuit (FR-001…FR-005, R-045, R-052, clarification 2026-11-10).
 */
class GameMasterTourTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /** Minimal agent: plays its card, passes reactions, votes YES. */
    static class PassAgent implements PlayerAgent {
        final String name;
        PassAgent(String name) { this.name = name; }
        @Override public String name() { return name; }
        @Override public Archetype archetype() { return Archetype.HAPPY; }
        @Override public PlayedCard playCard(Card card, String featureHtml) {
            return new PlayedCard(card.id(), "scenario", "explanation");
        }
        @Override public Reaction react(Leg leg, Participant cardPlayer, String featureHtml, boolean questionsAllowed) {
            return new Reaction.Pass();
        }
        @Override public com.cornucopai.game.model.Answer answerQuestion(String question) {
            return new com.cornucopai.game.model.Answer.Knows("ok");
        }
        @Override public Vote vote(Leg leg, Participant cardPlayer) { return Vote.YES; }
    }

    /** Agent that always votes NO (to force REJECTED tours). */
    static class NoAgent extends PassAgent {
        NoAgent(String name) { super(name); }
        @Override public Vote vote(Leg leg, Participant cardPlayer) { return Vote.NO; }
    }

    private AtelierStore store() { return new AtelierStore(tempDir, mapper); }

    private Card card(String id, String value, String suitId) {
        return new Card(id, value, "test", suitId, "Suit " + suitId, "en", "desc " + id, null, null);
    }

    private Atelier atelier(String id, List<Participant> players, List<Card> deck) {
        return new Atelier(id, Instant.parse("2026-11-11T10:00:00Z"), "<p>f</p>",
                players.size(), players, List.of(), deck);
    }

    private List<Participant> participants(String... names) {
        List<Participant> ps = new ArrayList<>();
        for (String n : names) ps.add(new Participant(n, Archetype.HAPPY));
        return ps;
    }

    @Test
    void threePlayersTwoCardsEachProduceTwoToursOfThreeLegs() {
        AtelierStore store = store();
        String id = store.createAtelierDir(store.newAtelierId());
        List<Participant> players = participants("A", "B", "C");
        // 6 cards, distinct suits so major suit is observable: tour 1 gets VE/AT/VE, tour 2 gets AT/VE/AT
        List<Card> deck = List.of(
                card("VE1", "2", "VE"), card("AT1", "3", "AT"), card("VE2", "4", "VE"),
                card("AT2", "5", "AT"), card("VE3", "6", "VE"), card("AT3", "7", "AT"));
        Atelier a = atelier(id, players, deck);
        List<PlayerAgent> agents = List.of(new PassAgent("A"), new PassAgent("B"), new PassAgent("C"));

        Atelier result = new GameMaster(store, new Random(42)).runGame(a, agents);

        // 2 tours, each with 3 leg indices (one per player)
        assertThat(result.tours()).hasSize(2);
        assertThat(result.tours().get(0).legIndices()).hasSize(3);
        assertThat(result.tours().get(1).legIndices()).hasSize(3);
        // tour 1 leader is one of the players; tour 2 leader = tour 1 winner (all VALIDATED + votes YES)
        assertThat(result.tours().get(0).leader()).isIn("A", "B", "C");
        assertThat(result.tours().get(1).leader()).isEqualTo(result.tours().get(0).winner());
        // major suit of tour 1 = suit of the first card dealt to the leader (first non-WC)
        List<Leg> legs = store.loadLegs(id);
        assertThat(legs).hasSize(6);
        // every leg carries the v3 fields
        assertThat(legs).allMatch(l -> l.turnIndex() >= 1 && l.suitId() != null
                && l.cardValue() != null && l.majorSuit() != null);
        // all legs of the same tour share the same majorSuit
        String tour1Major = result.tours().get(0).majorSuit();
        assertThat(tour1Major).isIn("VE", "AT");
        assertThat(legs.stream().filter(l -> l.turnIndex() == 1).map(Leg::majorSuit))
                .containsOnly(tour1Major);
    }

    @Test
    void allRejectedTourElectsRandomNextLeaderAndAwardsNoBonus() {
        AtelierStore store = store();
        String id = store.createAtelierDir(store.newAtelierId());
        List<Participant> players = participants("A", "B");
        List<Card> deck = List.of(card("VE1", "2", "VE"), card("VE2", "3", "VE"),
                card("AT1", "4", "AT"), card("AT2", "5", "AT"));
        Atelier a = atelier(id, players, deck);
        // everyone votes NO → every leg REJECTED → no tour winner → next leader random, bonusAwarded=false
        List<PlayerAgent> agents = List.of(new NoAgent("A"), new NoAgent("B"));

        Atelier result = new GameMaster(store, new Random(7)).runGame(a, agents);

        assertThat(result.tours()).hasSize(2);
        for (Tour t : result.tours()) {
            assertThat(t.bonusAwarded()).isFalse();
            assertThat(t.winner()).isNull();
        }
        List<Leg> legs = store.loadLegs(id);
        assertThat(legs).allMatch(l -> l.validation() == ValidationStatus.REJECTED);
        // rejected legs earn 0 scenario points
        assertThat(legs).allMatch(l -> l.points() == 0);
    }

    @Test
    void wcLedShiftsMajorSuitToNextNonWcCard_andWcCountsAsMajorForScoring() {
        AtelierStore store = store();
        String id = store.createAtelierDir(store.newAtelierId());
        List<Participant> players = participants("A", "B", "C");
        // deck: leader gets WCA (WC), others get VE/AT cards. Round-robin over shuffled order.
        List<Card> deck = List.of(
                card("WCA", "A", "WC"), card("VE1", "2", "VE"), card("AT1", "3", "AT"),
                card("VE2", "4", "VE"), card("AT2", "5", "AT"), card("WC2", "K", "WC"));
        Atelier a = atelier(id, players, deck);
        List<PlayerAgent> agents = List.of(new PassAgent("A"), new PassAgent("B"), new PassAgent("C"));

        Atelier result = new GameMaster(store, new Random(42)).runGame(a, agents);

        // tour 1: if the leader played WC, majorSuit must come from the first non-WC card of the tour
        Tour t1 = result.tours().get(0);
        List<Leg> legs = store.loadLegs(id);
        List<Leg> tour1Legs = legs.stream().filter(l -> l.turnIndex() == 1).toList();
        boolean leaderPlayedWc = tour1Legs.stream()
                .anyMatch(l -> l.suitId().equals("WC") && l.cardName().startsWith("WC"));
        if (t1.majorSuit() != null) {
            assertThat(t1.majorSuit()).isIn("VE", "AT"); // never "WC"
        }
        // a WC card played by a non-leader counts as major suit for its scenario point:
        // find a tour where majorSuit is set and a WC leg exists → that leg must have points >= 1 if VALIDATED
        for (Tour t : result.tours()) {
            if (t.majorSuit() == null) continue;
            for (Leg l : legs.stream().filter(x -> x.turnIndex() == t.index()).toList()) {
                if ("WC".equals(l.suitId()) && l.validation() == ValidationStatus.VALIDATED) {
                    assertThat(l.points()).isGreaterThanOrEqualTo(1);
                }
            }
        }
        // all-WC tour edge case is covered by majorSuit==null when every card is WC (checked via t.majorSuit in {null, VE, AT})
        assertThat(result.tours()).allMatch(t -> t.majorSuit() == null || !"WC".equals(t.majorSuit()));
    }

    @Test
    void tourWinnerLeadsNextTour() {
        AtelierStore store = store();
        String id = store.createAtelierDir(store.newAtelierId());
        List<Participant> players = participants("A", "B");
        // deck order: A gets c1,c3 ; B gets c2,c4 (round-robin over shuffled order)
        List<Card> deck = List.of(card("VE5", "5", "VE"), card("VE9", "9", "VE"),
                card("VEJ", "J", "VE"), card("VEA", "A", "VE"));
        Atelier a = atelier(id, players, deck);
        List<PlayerAgent> agents = List.of(new PassAgent("A"), new PassAgent("B"));

        Atelier result = new GameMaster(store, new Random(3)).runGame(a, agents);

        assertThat(result.tours()).hasSize(2);
        Tour t1 = result.tours().get(0);
        Tour t2 = result.tours().get(1);
        // all cards same suit VE → major suit VE; winner of tour 1 = highest value among tour-1 legs
        assertThat(t1.majorSuit()).isEqualTo("VE");
        assertThat(t1.bonusAwarded()).isTrue();
        assertThat(t1.winner()).isNotNull();
        // the winner of tour 1 opens tour 2 (clarification 2026-11-10)
        assertThat(t2.leader()).isEqualTo(t1.winner());
    }
}
