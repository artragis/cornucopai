package com.cornucopai.game;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.model.Answer;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.PlayedCard;
import com.cornucopai.game.model.Reaction;
import com.cornucopai.game.model.Vote;

/**
 * Behavioral contract of a player agent (research R-001). Deterministic
 * archetypes implement it now; a LangChain4j-backed agent will implement the
 * same interface in a later iteration.
 */
public interface PlayerAgent {

    String name();

    Archetype archetype();

    /** FR-006: the current player plays a real {@link Card} (id + description) with a scenario and an explanation (R-016). */
    PlayedCard playCard(Card card, String featureHtml);

    /**
     * FR-008/FR-010: what this agent does when another player's leg is on the table.
     * When {@code questionsAllowed} is false (after a "Je sais pas"), the agent may
     * only return a Precision or a Pass — never a Question.
     */
    Reaction react(Leg leg, Participant cardPlayer, String featureHtml, boolean questionsAllowed);

    /** FR-010: the card player answers "Je sais" + answer or "Je sais pas". */
    Answer answerQuestion(String question);

    /** FR-011: "valides-tu ?" */
    Vote vote(Leg leg, Participant cardPlayer);
}
