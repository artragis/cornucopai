package com.cornucopai.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.model.Answer;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Atelier;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.LegEntry;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.PlayedCard;
import com.cornucopai.game.model.Reaction;
import com.cornucopai.game.model.ValidationStatus;
import com.cornucopai.game.model.Vote;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GameMasterTest {

  @TempDir Path tempDir;

  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  /** Scriptable test double: reactions/votes/answers are polled from queues. */
  static class StubAgent implements PlayerAgent {
    final String name;
    final List<String> playSequence;
    final Queue<Reaction> reactions = new ArrayDeque<>();
    final Queue<Vote> votes = new ArrayDeque<>();
    final Queue<Answer> answers = new ArrayDeque<>();
    final List<String> playedCards = new ArrayList<>();
    final List<Boolean> reactQuestionsAllowed = new ArrayList<>();
    int voteCalls = 0;

    StubAgent(String name, List<String> playSequence) {
      this.name = name;
      this.playSequence = playSequence;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public Archetype archetype() {
      return Archetype.HAPPY;
    }

    @Override
    public PlayedCard playCard(Card card, String featureHtml) {
      playedCards.add(card.id());
      playSequence.add(name);
      return new PlayedCard(card.id(), "scenario by " + name, "explanation by " + name);
    }

    @Override
    public Reaction react(
        Leg leg, Participant cardPlayer, String featureHtml, boolean questionsAllowed) {
      reactQuestionsAllowed.add(questionsAllowed);
      return reactions.isEmpty() ? new Reaction.Pass() : reactions.poll();
    }

    @Override
    public Answer answerQuestion(String question) {
      return answers.isEmpty() ? new Answer.Knows("réponse par défaut") : answers.poll();
    }

    @Override
    public Vote vote(Leg leg, Participant cardPlayer) {
      voteCalls++;
      return votes.isEmpty() ? Vote.YES : votes.poll();
    }
  }

  private AtelierStore store() {
    return new AtelierStore(tempDir, mapper);
  }

  private Atelier newAtelier(String id, List<Participant> players, List<Card> deck) {
    return new Atelier(
        id,
        Instant.parse("2026-11-07T17:30:00Z"),
        "<p>feature</p>",
        players.size(),
        players,
        List.of(),
        deck);
  }

  private List<Card> cards(String... ids) {
    List<Card> out = new ArrayList<>();
    for (String id : ids) {
      out.add(new Card(id, id, "test", "T", "Test suit", "en", "desc for " + id, null, null));
    }
    return out;
  }

  private List<Card> deck8() {
    return cards("c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8");
  }

  private List<Participant> participants(String... names) {
    List<Participant> ps = new ArrayList<>();
    for (String n : names) {
      ps.add(new Participant(n, Archetype.HAPPY));
    }
    return ps;
  }

  private GameMaster gameMaster() {
    return new GameMaster(store(), new Random(42));
  }

  private List<PlayerAgent> agents(List<StubAgent> stubs) {
    return stubs.stream().map(s -> (PlayerAgent) s).collect(Collectors.toList());
  }

  @Test
  void fullGameProducesOneLegPerCardWithFinalValidationAndStableTurnOrder() {
    AtelierStore store = store();
    String id = store.createAtelierDir(store.newAtelierId());
    List<Participant> players = participants("Joueur-1", "Joueur-2", "Joueur-3", "Joueur-4");
    Atelier atelier = newAtelier(id, players, deck8());
    List<String> playSequence = new ArrayList<>();
    List<StubAgent> agents =
        players.stream().map(p -> new StubAgent(p.name(), playSequence)).toList();
    // everyone passes (default) and votes YES (default) → every leg VALIDATED

    Atelier result = gameMaster().runGame(atelier, agents(agents));

    // turn order drawn once, is a permutation of player names; play sequence cycles through it (4
    // players × 2 cards)
    assertThat(result.turnOrder())
        .hasSize(4)
        .containsExactlyInAnyOrderElementsOf(players.stream().map(Participant::name).toList());
    assertThat(playSequence).hasSize(8);
    List<String> expected = new ArrayList<>();
    expected.addAll(result.turnOrder());
    expected.addAll(result.turnOrder());
    assertThat(playSequence).containsExactlyElementsOf(expected);

    // one leg file per card, final validation (never PENDING), finishedAt set, atelier.json
    // persisted
    List<Leg> legs = store.loadLegs(id);
    assertThat(legs).hasSize(8);
    assertThat(legs).allMatch(m -> m.validation() == ValidationStatus.VALIDATED);
    assertThat(result.finishedAt()).isNotNull();
    assertThat(store.loadAtelier(id)).isPresent();
  }

  @Test
  void strictMajorityRejectsWhenNotStrictlyMoreThanHalfVoteYes() {
    AtelierStore store = store();
    String id = store.createAtelierDir(store.newAtelierId());
    List<Participant> players = participants("A", "B", "C");
    List<StubAgent> agents =
        List.of(
            new StubAgent("A", new ArrayList<>()),
            new StubAgent("B", new ArrayList<>()),
            new StubAgent("C", new ArrayList<>()));
    // A always YES, B and C always NO → each leg has 2 voters with at most 1 yes → never strictly
    // more than half → REJECTED
    agents.get(0).votes.addAll(List.of(Vote.YES, Vote.YES, Vote.YES));
    agents.get(1).votes.addAll(List.of(Vote.NO, Vote.NO, Vote.NO));
    agents.get(2).votes.addAll(List.of(Vote.NO, Vote.NO, Vote.NO));

    Atelier atelier = newAtelier(id, players, cards("c1", "c2"));
    gameMaster().runGame(atelier, agents(agents));

    List<Leg> legs = store.loadLegs(id);
    assertThat(legs).hasSize(2);
    assertThat(legs).allMatch(m -> m.validation() == ValidationStatus.REJECTED);
  }

  @Test
  void questionAnsweredWithKnowsAppendsAnswerToPrecisions() {
    StubAgent a = new StubAgent("A", new ArrayList<>());
    StubAgent b = new StubAgent("B", new ArrayList<>());
    // symmetric scripts: whoever is NOT the card player asks a question, and the card player
    // answers "Je sais"
    a.reactions.add(new Reaction.Question("Q de A"));
    b.reactions.add(new Reaction.Question("Q de B"));
    a.answers.add(new Answer.Knows("réponse de A"));
    b.answers.add(new Answer.Knows("réponse de B"));

    AtelierStore store = store();
    String id = store.createAtelierDir(store.newAtelierId());
    List<Participant> players = participants("A", "B");
    Atelier atelier = newAtelier(id, players, cards("c1"));
    Atelier result = gameMaster().runGame(atelier, agents(List.of(a, b)));

    String cardPlayer = result.turnOrder().get(0);
    String expectedAnswer = cardPlayer.equals("A") ? "réponse de A" : "réponse de B";
    List<Leg> legs = store.loadLegs(id);
    assertThat(legs).hasSize(1);
    // spec 005 v2: the question + answer persist as ONE QUESTION entry with knows/answer (R-037)
    assertThat(legs.get(0).entries()).hasSize(1);
    LegEntry entry = legs.get(0).entries().get(0);
    assertThat(entry.kind()).isEqualTo(LegEntry.Kind.QUESTION);
    assertThat(entry.knows()).isTrue();
    assertThat(entry.answer()).isEqualTo(expectedAnswer);
    // 2 players → 1 voter, votes YES → 1*2 > 1 → VALIDATED
    assertThat(legs.get(0).validation()).isEqualTo(ValidationStatus.VALIDATED);
  }

  @Test
  void doesNotKnowReasksWithoutQuestionsAndAppendsPrecision() {
    StubAgent a = new StubAgent("A", new ArrayList<>());
    StubAgent b = new StubAgent("B", new ArrayList<>());
    // symmetric: the non-card player asks, the card player does not know, the asker is re-asked (no
    // questions) and precises
    a.reactions.add(new Reaction.Question("Q-A"));
    a.reactions.add(new Reaction.Precision("P-A"));
    b.reactions.add(new Reaction.Question("Q-B"));
    b.reactions.add(new Reaction.Precision("P-B"));
    a.answers.add(new Answer.DoesNotKnow());
    b.answers.add(new Answer.DoesNotKnow());

    AtelierStore store = store();
    String id = store.createAtelierDir(store.newAtelierId());
    List<Participant> players = participants("A", "B");
    Atelier atelier = newAtelier(id, players, cards("c1"));
    Atelier result = gameMaster().runGame(atelier, agents(List.of(a, b)));

    String cardPlayer = result.turnOrder().get(0);
    StubAgent asker = cardPlayer.equals("A") ? b : a;
    String expectedPrecision = cardPlayer.equals("A") ? "P-B" : "P-A";
    assertThat(asker.reactQuestionsAllowed).containsExactly(true, false);
    List<Leg> legs = store.loadLegs(id);
    // spec 005 v2: QUESTION entry with knows=false, then the PRECISION entry (R-037)
    assertThat(legs.get(0).entries()).hasSize(2);
    assertThat(legs.get(0).entries().get(0).kind()).isEqualTo(LegEntry.Kind.QUESTION);
    assertThat(legs.get(0).entries().get(0).knows()).isFalse();
    assertThat(legs.get(0).entries().get(1).kind()).isEqualTo(LegEntry.Kind.PRECISION);
    assertThat(legs.get(0).entries().get(1).text()).isEqualTo(expectedPrecision);
    assertThat(legs.get(0).entries().get(1).author()).isEqualTo(asker.name);
  }

  @Test
  void questionAfterDoesNotKnowIsRejected() {
    StubAgent a = new StubAgent("A", new ArrayList<>());
    StubAgent b = new StubAgent("B", new ArrayList<>());
    a.reactions.add(new Reaction.Question("Q1-A"));
    a.reactions.add(new Reaction.Question("Q2-A interdite"));
    b.reactions.add(new Reaction.Question("Q1-B"));
    b.reactions.add(new Reaction.Question("Q2-B interdite"));
    a.answers.add(new Answer.DoesNotKnow());
    b.answers.add(new Answer.DoesNotKnow());

    AtelierStore store = store();
    String id = store.createAtelierDir(store.newAtelierId());
    List<Participant> players = participants("A", "B");
    Atelier atelier = newAtelier(id, players, cards("c1"));
    assertThatThrownBy(() -> gameMaster().runGame(atelier, agents(List.of(a, b))))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void voteIsOnlyCollectedFromOtherPlayers() {
    AtelierStore store = store();
    String id = store.createAtelierDir(store.newAtelierId());
    List<Participant> players = participants("A", "B", "C");
    Atelier atelier = newAtelier(id, players, cards("c1"));
    List<StubAgent> agents =
        List.of(
            new StubAgent("A", new ArrayList<>()),
            new StubAgent("B", new ArrayList<>()),
            new StubAgent("C", new ArrayList<>()));
    // everyone votes YES by default → VALIDATED (2 yes of 2 voters)

    Atelier result = gameMaster().runGame(atelier, agents(agents));

    String cardPlayer = result.turnOrder().get(0);
    for (StubAgent agent : agents) {
      if (agent.name.equals(cardPlayer)) {
        assertThat(agent.voteCalls)
            .isZero(); // the card player is never asked to vote on their own leg
      } else {
        assertThat(agent.voteCalls).isEqualTo(1);
      }
    }
    List<Leg> legs = store.loadLegs(id);
    assertThat(legs.get(0).validation()).isEqualTo(ValidationStatus.VALIDATED);
  }
}
