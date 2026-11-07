package com.cornucopai.game;

import com.cornucopai.game.model.Answer;
import com.cornucopai.game.model.Atelier;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.PlayedCard;
import com.cornucopai.game.model.Reaction;
import com.cornucopai.game.model.ValidationStatus;
import com.cornucopai.game.model.Vote;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The game master (research R-009, spec FR-005..FR-013): draws the turn order
 * once, then loops over turns — play a card (leg persisted PENDING), collect
 * reactions (precision / question / pass) from each other player, let the card
 * player answer questions, collect votes, and flip the leg to VALIDATED
 * (strict majority of yes among the voters) or REJECTED. The game ends when all
 * hands are empty.
 */
public class GameMaster {

    private static final Logger LOG = Logger.getLogger(GameMaster.class);

    private final AtelierStore store;
    private final PlaceholderCards cards;
    private final Random random;

    public GameMaster(AtelierStore store, PlaceholderCards cards, Random random) {
        this.store = store;
        this.cards = cards;
        this.random = random;
    }

    public Atelier runGame(Atelier atelier, List<PlayerAgent> agents) {
        Map<String, PlayerAgent> byName = agents.stream()
                .collect(Collectors.toMap(PlayerAgent::name, Function.identity()));
        Map<String, Participant> participants = atelier.players().stream()
                .collect(Collectors.toMap(Participant::name, Function.identity()));

        // FR-005: draw the turn order once and keep it for the whole game
        List<String> order = new ArrayList<>(participants.keySet());
        Collections.shuffle(order, random);
        atelier.setTurnOrder(order);
        store.saveAtelier(atelier.id(), atelier);
        LOG.infof("Atelier %s — turn order: %s", atelier.id(), order);

        Map<String, Deque<String>> hands = cards.deal(order, atelier.deck()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new ArrayDeque<>(e.getValue())));

        int turn = 0;
        int pointer = 0;
        while (hands.values().stream().anyMatch(h -> !h.isEmpty())) {
            String current = nextPlayerWithCards(order, hands, pointer);
            if (current == null) {
                break;
            }
            pointer = (order.indexOf(current) + 1) % order.size();
            turn++;
            runTurn(atelier, byName, participants, hands, order, current, turn);
        }

        atelier.setFinishedAt(Instant.now());
        store.saveAtelier(atelier.id(), atelier);
        LOG.infof("Atelier %s finished after %d legs", atelier.id(), turn);
        return atelier;
    }

    private String nextPlayerWithCards(List<String> order, Map<String, Deque<String>> hands, int pointer) {
        for (int i = 0; i < order.size(); i++) {
            String candidate = order.get((pointer + i) % order.size());
            if (!hands.get(candidate).isEmpty()) {
                return candidate;
            }
        }
        return null;
    }

    private void runTurn(Atelier atelier, Map<String, PlayerAgent> byName, Map<String, Participant> participants,
                         Map<String, Deque<String>> hands, List<String> order, String current, int turn) {
        PlayerAgent cardAgent = byName.get(current);
        Participant cardPlayer = participants.get(current);
        String card = hands.get(current).poll();

        // FR-006/FR-007: the current player gives card + scenario + explanation; leg persisted PENDING
        PlayedCard played = cardAgent.playCard(card, atelier.featureHtml());
        Leg leg = new Leg(played.cardName(), played.scenario(), played.explanation());
        store.saveLeg(atelier.id(), turn, current, leg);
        LOG.infof("Turn %d — %s plays %s (%s)", turn, current, leg.cardName(), leg.scenario());

        // FR-008..FR-010: each other player reacts, one by one
        for (String otherName : order) {
            if (otherName.equals(current)) {
                continue;
            }
            PlayerAgent other = byName.get(otherName);
            boolean questionsAllowed = true;
            Reaction reaction = other.react(leg, cardPlayer, atelier.featureHtml(), true);
            while (true) {
                if (reaction instanceof Reaction.Precision precision) {
                    leg.addPrecision(precision.text());
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
                        leg.addPrecision(knows.answer());
                        store.saveLeg(atelier.id(), turn, current, leg);
                        LOG.infof("Turn %d — %s asked, %s knows: %s", turn, otherName, current, knows.answer());
                        break;
                    }
                    // "Je sais pas": re-ask the same player, who can no longer ask questions
                    LOG.infof("Turn %d — %s asked, %s does not know; re-asking %s without questions",
                            turn, otherName, current, otherName);
                    questionsAllowed = false;
                    reaction = other.react(leg, cardPlayer, atelier.featureHtml(), false);
                } else {
                    break; // Pass
                }
            }
        }

        // FR-011/FR-012: every other player votes; strict majority of yes among voters → VALIDATED
        int voters = 0;
        int yes = 0;
        for (String otherName : order) {
            if (otherName.equals(current)) {
                continue;
            }
            voters++;
            Vote vote = byName.get(otherName).vote(leg, cardPlayer);
            if (vote == Vote.YES) {
                yes++;
            }
        }
        leg.setValidation(yes * 2 > voters ? ValidationStatus.VALIDATED : ValidationStatus.REJECTED);
        store.saveLeg(atelier.id(), turn, current, leg);
        LOG.infof("Turn %d — vote %d/%d yes → %s", turn, yes, voters, leg.validation());
    }
}
