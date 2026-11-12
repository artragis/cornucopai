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
 * Behavioral contract of a player agent (research R-001). Deterministic archetypes implement it
 * now; a LangChain4j-backed agent will implement the same interface in a later iteration.
 */
public interface PlayerAgent {

  String name();

  Archetype archetype();

  /**
   * FR-006: the current player plays a real {@link Card} (id + description) with a scenario and an
   * explanation (R-016).
   */
  PlayedCard playCard(Card card, String featureHtml);

  /**
   * FR-008/FR-010: what this agent does when another player's leg is on the table. When {@code
   * questionsAllowed} is false (after a "Je sais pas"), the agent may only return a Precision or a
   * Pass — never a Question.
   */
  Reaction react(Leg leg, Participant cardPlayer, String featureHtml, boolean questionsAllowed);

  /** FR-010: the card player answers "Je sais" + answer or "Je sais pas". */
  Answer answerQuestion(String question);

  /** FR-011: "valides-tu ?". */
  Vote vote(Leg leg, Participant cardPlayer);

  /**
   * Spec 007 US3 (FR-009/FR-010, R-051): the vote with its structured decision-tree justification.
   * Default wraps {@link #vote} with neutral checks; LLM agents override.
   */
  default com.cornucopai.game.model.VoteDecision voteWithDecision(Leg leg, Participant cardPlayer) {
    Vote v = vote(leg, cardPlayer);
    return new com.cornucopai.game.model.VoteDecision(
        name(),
        v == Vote.YES
            ? com.cornucopai.game.model.VoteChoice.YES
            : com.cornucopai.game.model.VoteChoice.NO,
        com.cornucopai.game.model.ExplanationCheck.OK,
        com.cornucopai.game.model.ApplicabilityCheck.OK,
        "");
  }

  /**
   * Spec 007 US5 (FR-013, R-050/R-052): one free-text debate argument before voting. Default
   * returns an empty string (no debate contribution).
   */
  default String debateArgument(Leg leg, Participant cardPlayer) {
    return "";
  }

  /**
   * Spec 007 US5 (R-052): the per-game {@code LegContext} shared by the agents, so the {@code
   * GameMaster} can run the debate and the agents see the same debate turns. Default null
   * (deterministic agents have no LLM context).
   */
  default com.cornucopai.game.llm.LegContext legContext() {
    return null;
  }

  /**
   * Spec 007 US2 (FR-008, review 2026-11-11): the per-atelier shared project memory. Default null;
   * LLM agents override. The {@code GameMaster} snapshots it into the atelier after each turn so
   * discovered facts are persisted and can be inherited by a later run.
   */
  default com.cornucopai.game.memory.SharedProjectMemory sharedMemory() {
    return null;
  }
}
