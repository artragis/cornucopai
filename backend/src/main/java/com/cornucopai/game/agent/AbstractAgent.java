package com.cornucopai.game.agent;

import com.cornucopai.game.PlayerAgent;
import com.cornucopai.game.model.Answer;

/**
 * Shared base of the deterministic archetype agents.
 *
 * <p>Spec 004 (FR-003): deterministic agents are kept for UNIT TESTS ONLY — games are played by
 * {@link LlmPlayerAgent}.
 */
abstract class AbstractAgent implements PlayerAgent {

  private final String name;

  protected AbstractAgent(String name) {
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Answer answerQuestion(String question) {
    return new Answer.Knows(
        "Je sais : la réponse à « " + question + " » est documentée dans l'analyse de risques.");
  }
}
