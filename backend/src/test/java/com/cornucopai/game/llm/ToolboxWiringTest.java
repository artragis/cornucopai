package com.cornucopai.game.llm;

import com.cornucopai.game.card.CardInfoService;
import com.cornucopai.game.card.CardInfoTool;
import com.cornucopai.game.model.Archetype;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T031 (US3): the toolbox given to the factory is registered on every LangChain4j
 * brain (FR-005). The CDI path wires the same CardInfoTool via @Inject method
 * (exercised by every @QuarkusTest boot); here the constructor path is asserted.
 */
class ToolboxWiringTest {

    @Test
    void cardInfoToolIsRegisteredOnTheBrain() {
        CardInfoTool tool = new CardInfoTool(new CardInfoService(Path.of("src/test/resources/cornucopia-fixture")));
        LlmDefaults defaults = new LlmDefaults("http://localhost:1235/v1", null, null, Duration.ofSeconds(5));
        LlmBrainFactory factory = new LlmBrainFactory("auto", defaults, List.of(tool));
        LlmBrain brain = factory.create(AgentLlmConfig.defaultOpenAiCompatible("http://localhost:1235/v1"),
                "Joueur-1", Archetype.GRUMPY, "atelierX");
        assertThat(brain).isInstanceOf(LangChain4jBrain.class);
        assertThat(((LangChain4jBrain) brain).tools()).contains(tool);
    }
}
