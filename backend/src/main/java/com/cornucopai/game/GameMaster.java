package com.cornucopai.game;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.model.Answer;
import com.cornucopai.game.model.Atelier;
import com.cornucopai.game.model.CardValueOrder;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.LegEntry;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.PlayedCard;
import com.cornucopai.game.model.Reaction;
import com.cornucopai.game.model.Tour;
import com.cornucopai.game.model.ValidationStatus;
import com.cornucopai.game.model.Vote;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The game master (research R-009, spec FR-005..FR-013) extended by spec 006:
 * legs are grouped into <b>tours</b> (tricks) — one leg per player still holding
 * cards per tour. Tour 1 leader is drawn at random; the next tour's leader is the
 * previous tour's winner (highest major-suit card), or a random draw if all legs of
 * the tour were REJECTED (clarification 2026-11-10). The first non-WC card of a tour
 * sets its <b>couleur majeure</b>; a WC card is always treated as the major suit
 * (joker, FR-004). Scoring: +1 per VALIDATED leg in the major suit, +1 bonus per tour
 * to the highest major-suit card (FR-006..FR-011).
 */
public class GameMaster {

    private static final Logger LOG = Logger.getLogger(GameMaster.class);

    /** Joker family id — Cornucopia / Wild Card (R-052). */
    static final String WC_SUIT = "WC";

    private final AtelierStore store;
    private final Random random;
    /** Spec 007 US5 (FR-013): whether the bounded pre-vote debate runs (OFF by default since 2026-11-11). */
    private final boolean debateEnabled;
    private final com.cornucopai.game.debate.DebateOrchestrator debateOrchestrator = new com.cornucopai.game.debate.DebateOrchestrator();

    public GameMaster(AtelierStore store, Random random) {
        this(store, random, false);
    }

    public GameMaster(AtelierStore store, Random random, boolean debateEnabled) {
        this.store = store;
        this.random = random;
        this.debateEnabled = debateEnabled;
    }

    public Atelier runGame(Atelier atelier, List<PlayerAgent> agents) {
        Map<String, PlayerAgent> byName = agents.stream()
                .collect(Collectors.toMap(PlayerAgent::name, Function.identity()));
        Map<String, Participant> participants = atelier.players().stream()
                .collect(Collectors.toMap(Participant::name, Function.identity()));

        // FR-005: draw the base turn order once and keep it for the whole game
        List<String> order = new ArrayList<>(participants.keySet());
        Collections.shuffle(order, random);
        atelier.setTurnOrder(order);
        store.saveAtelier(atelier.id(), atelier);
        LOG.infof("Atelier %s — base turn order: %s", atelier.id(), order);

        // Spec 003 (R-018): deal the real-card deck round-robin over the base order
        Map<String, Deque<Card>> hands = new LinkedHashMap<>();
        for (String player : order) {
            hands.put(player, new ArrayDeque<>());
        }
        int i = 0;
        for (Card card : atelier.deck()) {
            hands.get(order.get(i % order.size())).add(card);
            i++;
        }

        // Spec 006: tour 1 leader drawn at random (FR-001, clarification 2026-11-10)
        String leader = order.get(random.nextInt(order.size()));
        int turn = 0;
        int tourIndex = 0;
        List<Tour> tours = new ArrayList<>();
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (String p : order) {
            scores.put(p, 0);
        }

        while (hands.values().stream().anyMatch(h -> !h.isEmpty())) {
            tourIndex++;
            // build the tour's player sequence: leader first, then the base order, skipping empty hands
            List<String> tourPlayers = new ArrayList<>();
            int leaderIdx = order.indexOf(leader);
            for (int k = 0; k < order.size(); k++) {
                String p = order.get((leaderIdx + k) % order.size());
                if (!hands.get(p).isEmpty()) {
                    tourPlayers.add(p);
                }
            }
            if (tourPlayers.isEmpty()) {
                break;
            }

            String majorSuit = null;
            List<Integer> legIndices = new ArrayList<>();
            List<LegPlay> plays = new ArrayList<>();
            for (String current : tourPlayers) {
                turn++;
                legIndices.add(turn);
                Card card = hands.get(current).poll();
                // couleur majeure: first non-WC card of the tour (FR-002/FR-003); WC led shifts to next non-WC
                if (majorSuit == null && !WC_SUIT.equals(card.suitId())) {
                    majorSuit = card.suitId();
                }
                LegPlay play = runTurn(atelier, byName, participants, order, current, card, turn, tourIndex);
                plays.add(play);
            }

            closeTour(atelier, tourIndex, leader, majorSuit, legIndices, plays, scores, tours);

            // next leader = tour winner, or random if all legs REJECTED (no winner)
            Tour justClosed = tours.get(tours.size() - 1);
            if (justClosed.winner() != null) {
                leader = justClosed.winner();
            } else {
                leader = order.get(random.nextInt(order.size()));
            }
        }

        // final scores + winner (FR-011): max score, ties included
        atelier.setTours(tours);
        atelier.setScores(scores);
        int max = scores.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<String> winners = scores.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .toList();
        atelier.setWinner(winners);
        atelier.setFinishedAt(Instant.now());
        store.saveAtelier(atelier.id(), atelier);
        LOG.infof("Atelier %s finished after %d legs in %d tours — winner(s): %s (scores %s)",
                atelier.id(), turn, tourIndex, winners, scores);
        return atelier;
    }

    /** One played card + its player, so the tour loop can finalize scoring. */
    private record LegPlay(String player, Card card, Leg leg, int turn) {
    }

    /**
     * Runs one leg: play card (persisted PENDING with v3 fields), collect reactions,
     * answer questions, collect votes, flip validation. Returns the play for tour
     * finalization. majorSuit may still be null here (set retroactively at tour close).
     */
    private LegPlay runTurn(Atelier atelier, Map<String, PlayerAgent> byName,
                            Map<String, Participant> participants, List<String> order,
                            String current, Card card, int turn, int tourIndex) {
        PlayerAgent cardAgent = byName.get(current);
        Participant cardPlayer = participants.get(current);

        PlayedCard played = cardAgent.playCard(card, atelier.featureHtml());
        Leg leg = new Leg(played.cardName(), played.scenario(), played.explanation());
        // spec 006 v3 fields known at play time (FR-018)
        leg.setTurnIndex(tourIndex);
        leg.setSuitId(card.suitId());
        leg.setCardValue(card.value());
        store.saveLeg(atelier.id(), turn, current, leg);
        LOG.infof("Tour %d, turn %d — %s plays %s (%s, suit=%s, value=%s)",
                tourIndex, turn, current, leg.cardName(), leg.scenario(), card.suitId(), card.value());

        // FR-008..FR-010: each other player in the tour reacts, one by one
        for (String otherName : order) {
            if (otherName.equals(current)) {
                continue;
            }
            PlayerAgent other = byName.get(otherName);
            boolean questionsAllowed = true;
            Reaction reaction = other.react(leg, cardPlayer, atelier.featureHtml(), true);
            while (true) {
                if (reaction instanceof Reaction.Precision precision) {
                    leg.addEntry(LegEntry.precision(otherName, precision.text()));
                    store.saveLeg(atelier.id(), turn, current, leg);
                    LOG.infof("Turn %d — %s adds a precision", turn, otherName);
                    break;
                } else if (reaction instanceof Reaction.Question question) {
                    if (!questionsAllowed) {
                        throw new IllegalStateException(
                                otherName + " asked a question although forbidden after a 'Je sais pas'");
                    }
                    Answer answer = cardAgent.answerQuestion(question.text());
                    if (answer instanceof Answer.Knows knows) {
                        leg.addEntry(LegEntry.question(otherName, question.text(), true, knows.answer()));
                        store.saveLeg(atelier.id(), turn, current, leg);
                        LOG.infof("Turn %d — %s asked, %s knows: %s", turn, otherName, current, knows.answer());
                        break;
                    }
                    leg.addEntry(LegEntry.question(otherName, question.text(), false, null));
                    store.saveLeg(atelier.id(), turn, current, leg);
                    LOG.infof("Turn %d — %s asked, %s does not know; re-asking %s without questions",
                            turn, otherName, current, otherName);
                    questionsAllowed = false;
                    reaction = other.react(leg, cardPlayer, atelier.featureHtml(), false);
                } else {
                    break; // Pass
                }
            }
        }

        // Spec 007 US5 (FR-013, R-052): bounded pre-vote debate, only when enabled (OFF by default since 2026-11-11)
        com.cornucopai.game.llm.LegContext legContext = cardAgent.legContext();
        if (debateEnabled && legContext != null) {
            List<PlayerAgent> voters = order.stream()
                    .filter(n -> !n.equals(current))
                    .map(byName::get)
                    .toList();
            legContext.setCurrent(leg);
            debateOrchestrator.runDebate(leg, voters, cardPlayer, legContext);
        } else if (legContext != null) {
            legContext.setDebateTurns(List.of());
        }

        // FR-011/FR-012 + spec 007 US3 (FR-010): every other player votes with a structured
        // decision-tree justification; strict majority of yes among voters → VALIDATED
        int voters = 0;
        int yes = 0;
        for (String otherName : order) {
            if (otherName.equals(current)) {
                continue;
            }
            voters++;
            com.cornucopai.game.model.VoteDecision decision = byName.get(otherName).voteWithDecision(leg, cardPlayer);
            List<com.cornucopai.game.model.VoteDecision> votes = new ArrayList<>(leg.votes());
            votes.add(decision);
            leg.setVotes(votes);
            if (decision.vote() == com.cornucopai.game.model.VoteChoice.YES) {
                yes++;
            }
        }
        leg.setValidation(yes * 2 > voters ? ValidationStatus.VALIDATED : ValidationStatus.REJECTED);
        store.saveLeg(atelier.id(), turn, current, leg);
        LOG.infof("Turn %d — vote %d/%d yes → %s", turn, yes, voters, leg.validation());

        persistFacts(atelier, byName);
        return new LegPlay(current, card, leg, turn);
    }

    /**
     * Spec 007 US2 (FR-008, review 2026-11-11): after each turn, snapshot the agents' shared
     * project memory into the atelier and persist it, so discovered facts survive a restart
     * (atelier.json) and can be inherited by a later run via {@code inheritFactsFrom}.
     */
    private void persistFacts(Atelier atelier, Map<String, PlayerAgent> byName) {
        for (PlayerAgent agent : byName.values()) {
            com.cornucopai.game.memory.SharedProjectMemory memory = agent.sharedMemory();
            if (memory != null) {
                atelier.setProjectFacts(new ArrayList<>(memory.facts()));
                store.saveAtelier(atelier.id(), atelier);
                return;
            }
        }
    }

    /**
     * Closes a tour: sets the final majorSuit on every leg of the tour, computes the
     * scenario point per leg (+1 if VALIDATED and in the major suit — WC counts as
     * major, FR-004/FR-006), awards the +1 bonus to the highest major-suit card
     * (FR-009, CardValueOrder), updates scores, persists legs + the Tour.
     */
    private void closeTour(Atelier atelier, int tourIndex, String leader, String majorSuit,
                           List<Integer> legIndices, List<LegPlay> plays,
                           Map<String, Integer> scores, List<Tour> tours) {
        String winner = null;
        boolean bonusAwarded = false;
        int bestRank = -1;
        LegPlay bestPlay = null;

        for (LegPlay play : plays) {
            Leg leg = play.leg();
            leg.setMajorSuit(majorSuit);
            boolean inMajor = majorSuit != null
                    && (majorSuit.equals(play.card().suitId()) || WC_SUIT.equals(play.card().suitId()));
            int points = (leg.validation() == ValidationStatus.VALIDATED && inMajor) ? 1 : 0;
            leg.setPoints(points);
            scores.merge(play.player(), points, Integer::sum);

            // bonus: highest card among major-suit cards (WC counts as major)
            if (inMajor) {
                int rank = CardValueOrder.rank(play.card().value());
                if (rank > bestRank) {
                    bestRank = rank;
                    bestPlay = play;
                }
            }
        }

        // clarification 2026-11-10: if ALL legs of the tour were REJECTED, there is no
        // tour winner — the next leader is drawn at random and no bonus is awarded
        boolean anyValidated = plays.stream().anyMatch(p -> p.leg().validation() == ValidationStatus.VALIDATED);
        if (bestPlay != null && anyValidated) {
            winner = bestPlay.player();
            bonusAwarded = true;
            bestPlay.leg().setPoints(bestPlay.leg().points() + 1);
            scores.merge(winner, 1, Integer::sum);
        }

        // re-persist legs with final majorSuit + points, then the atelier with the new tour
        for (LegPlay play : plays) {
            store.saveLeg(atelier.id(), play.turn(), play.player(), play.leg());
        }
        Tour tour = new Tour(tourIndex, leader, majorSuit, legIndices, winner, bonusAwarded);
        tours.add(tour);
        atelier.setTours(new ArrayList<>(tours));
        store.saveAtelier(atelier.id(), atelier);
        LOG.infof("Tour %d closed — majorSuit=%s, winner=%s, bonus=%s", tourIndex, majorSuit, winner, bonusAwarded);
    }
}
