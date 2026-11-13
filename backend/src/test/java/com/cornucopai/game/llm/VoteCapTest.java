package com.cornucopai.game.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * T017 (US3): the vote call is capped at 100 output tokens via a dedicated capped model inside the
 * brain (R-040, FR-006, SC-002). Cap raised from 16 to 100 because 16 broke the real LLM answers
 * too often (user decision, 2026-11-10).
 */
class VoteCapTest {

  @Test
  void voteMaxTokensIsCappedAt100() {
    LlmDefaults defaults =
        new LlmDefaults("http://localhost:1235/v1", null, null, Duration.ofSeconds(5));
    LlmBrainFactory factory = new LlmBrainFactory("auto", defaults, List.of());
    LangChain4jBrain brain =
        (LangChain4jBrain)
            factory.create(TestAgents.snapshot("agent-1"), "Joueur-1", "atelierX", List.of());
    assertThat(brain.maxCompletionTokens()).isEqualTo(100);
    assertThat(brain.hasDedicatedVoteModel()).isTrue();
  }
}
