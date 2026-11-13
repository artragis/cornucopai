package com.cornucopai.game.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.cornucopai.game.card.CardInfoService;
import com.cornucopai.game.card.CardInfoTool;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * T031/T012: the toolbox given to the factory is registered on every LangChain4j brain (FR-005,
 * spec 005 US2): base tools (CardInfoTool) + per-game tools (TourInfoTool). The CDI path wires the
 * base tool via @Inject method (exercised by every @QuarkusTest boot); here the constructor paths
 * are asserted.
 */
class ToolboxWiringTest {

  @Test
  void cardInfoToolIsRegisteredOnTheBrain() {
    CardInfoTool tool =
        new CardInfoTool(new CardInfoService(Path.of("src/test/resources/cornucopia-fixture")));
    LlmDefaults defaults =
        new LlmDefaults("http://localhost:1235/v1", null, null, Duration.ofSeconds(5));
    LlmBrainFactory factory = new LlmBrainFactory("auto", defaults, List.of(tool));
    LlmBrain brain =
        factory.create(TestAgents.snapshot("agent-1"), "Joueur-1", "atelierX", List.of());
    assertThat(brain).isInstanceOf(LangChain4jBrain.class);
    assertThat(((LangChain4jBrain) brain).tools()).contains(tool);
  }

  @Test
  void gameToolsAreRegisteredAlongsideBaseTools() {
    CardInfoTool cardTool =
        new CardInfoTool(new CardInfoService(Path.of("src/test/resources/cornucopia-fixture")));
    TourInfoTool tourTool = new TourInfoTool(new LegContext("atelierX"));
    LlmDefaults defaults =
        new LlmDefaults("http://localhost:1235/v1", null, null, Duration.ofSeconds(5));
    LlmBrainFactory factory = new LlmBrainFactory("auto", defaults, List.of(cardTool));
    LlmBrain brain =
        factory.create(TestAgents.snapshot("agent-1"), "Joueur-1", "atelierX", List.of(tourTool));
    assertThat(((LangChain4jBrain) brain).tools()).contains(cardTool, tourTool);
  }
}
