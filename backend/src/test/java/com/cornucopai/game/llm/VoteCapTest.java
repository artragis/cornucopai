package com.cornucopai.game.llm;

import com.cornucopai.game.model.Archetype;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T017 (US3): the vote call is capped at 100 output tokens via a dedicated capped model
 * inside the brain (R-040, FR-006, SC-002). Cap raised from 16 to 100 because 16 broke
 * the real LLM answers too often (user decision, 2026-11-10).
 */
class VoteCapTest {

    @Test
    void voteMaxTokensIsCappedAt100() {
        LlmDefaults defaults = new LlmDefaults("http://localhost:1235/v1", null, null, Duration.ofSeconds(5));
        LlmBrainFactory factory = new LlmBrainFactory("auto", defaults, List.of());
        LangChain4jBrain brain = (LangChain4jBrain) factory.create(
                AgentLlmConfig.defaultOpenAiCompatible("http://localhost:1235/v1"),
                "Joueur-1", Archetype.GRUMPY, "atelierX", List.of());
        assertThat(brain.maxCompletionTokens()).isEqualTo(100);
        assertThat(brain.hasDedicatedVoteModel()).isTrue();
    }
}
