package com.cornucopai.game.llm;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.model.chat.DisabledChatModel;
import dev.langchain4j.service.AiServices;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * R-065 (observed 2026-11-11 on a real run): inside Quarkus, {@code
 * AiServices.builder(...).build()} goes through {@code QuarkusAiServicesFactory}, which needs the
 * interface registered at build time. Without {@code @CreatedAware} on {@link PlayerAiService},
 * every build() failed with « Quarkus was not able to determine class ... as an AiService at build
 * time ». This test boots the real Quarkus runtime and performs the same build() call
 * LangChain4jBrain performs.
 */
@QuarkusTest
class PlayerAiServiceQuarkusBuildTest {

  @Test
  void aiServiceBuildsInsideQuarkusRuntime() {
    PlayerAiService service =
        AiServices.builder(PlayerAiService.class)
            .chatModel(new DisabledChatModel())
            .systemMessageProvider(id -> "system prompt")
            .build();
    assertThat(service).isNotNull();
  }
}
