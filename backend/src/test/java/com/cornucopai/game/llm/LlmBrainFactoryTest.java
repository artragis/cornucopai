package com.cornucopai.game.llm;

import com.cornucopai.game.model.Archetype;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** T022 (US2): the factory picks the brain from the mode and the provider type (R-029, R-031). */
class LlmBrainFactoryTest {

    private final LlmDefaults defaults = new LlmDefaults("http://localhost:1235/v1", null, null, Duration.ofSeconds(5));

    @Test
    void fakeModeReturnsTheScriptedBrain() {
        LlmBrainFactory factory = new LlmBrainFactory("fake", defaults, List.of());
        LlmBrain brain = factory.create(AgentLlmConfig.defaultOpenAiCompatible("http://localhost:1235/v1"),
                "Joueur-1", Archetype.GRUMPY, "atelierX");
        assertThat(brain).isInstanceOf(FakeLlmBrain.class);
    }

    @Test
    void autoModeMapsProviderTypeToBrain() {
        LlmBrainFactory factory = new LlmBrainFactory("auto", defaults, List.of());
        assertThat(factory.create(AgentLlmConfig.defaultOpenAiCompatible("http://localhost:1235/v1"),
                "Joueur-1", Archetype.GRUMPY, "atelierX")).isInstanceOf(LangChain4jBrain.class);
        assertThat(factory.create(new AgentLlmConfig(AgentLlmConfig.ProviderType.ACP, null, null, null, "mistral-vibe", null),
                "Joueur-2", Archetype.HAPPY, "atelierX")).isInstanceOf(AcpCliBrain.class);
    }
}
