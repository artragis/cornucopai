package com.cornucopai.game.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.cornucopai.game.model.Archetype;
import dev.langchain4j.data.message.UserMessage;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * T037 (US4): memory is one sliding window per agent per atelier — distinct instances, ids {@code
 * <atelierId>/<agentName>}, window capped at 40 messages, nothing shared or persisted across
 * ateliers (FR-010, R-025).
 */
class MemoryIsolationTest {

  private final LlmDefaults defaults =
      new LlmDefaults("http://localhost:1235/v1", null, null, Duration.ofSeconds(5));
  private final LlmBrainFactory factory = new LlmBrainFactory("auto", defaults, List.of());
  private final AgentLlmConfig cfg =
      AgentLlmConfig.defaultOpenAiCompatible("http://localhost:1235/v1");

  @Test
  void eachAgentGetsItsOwnMemoryScopedToTheAtelier() {
    LangChain4jBrain a1 =
        (LangChain4jBrain) factory.create(cfg, "Joueur-1", Archetype.GRUMPY, "atelierA");
    LangChain4jBrain a2 =
        (LangChain4jBrain) factory.create(cfg, "Joueur-1", Archetype.GRUMPY, "atelierB");
    LangChain4jBrain b1 =
        (LangChain4jBrain) factory.create(cfg, "Joueur-2", Archetype.HAPPY, "atelierA");
    assertThat(a1.chatMemory()).isNotSameAs(a2.chatMemory());
    assertThat(a1.chatMemory()).isNotSameAs(b1.chatMemory());
    assertThat(a1.chatMemory().id()).isEqualTo("atelierA/Joueur-1");
    assertThat(a2.chatMemory().id()).isEqualTo("atelierB/Joueur-1");
    assertThat(b1.chatMemory().id()).isEqualTo("atelierA/Joueur-2");
  }

  @Test
  void memoryWindowIsCappedAt40Messages() {
    LangChain4jBrain brain =
        (LangChain4jBrain) factory.create(cfg, "Joueur-1", Archetype.GRUMPY, "atelierA");
    for (int i = 0; i < 50; i++) {
      brain.chatMemory().add(new UserMessage("message " + i));
    }
    assertThat(brain.chatMemory().messages()).hasSize(40);
    // sliding window: the oldest messages were evicted
    assertThat(brain.chatMemory().messages().get(0).toString()).contains("message 10");
  }
}
