package com.cornucopai.game.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.cornucopai.game.card.CardInfoService;
import com.cornucopai.game.card.CardInfoTool;
import com.cornucopai.game.mcp.McpIdeConfig;
import com.cornucopai.game.mcp.McpToolProviderFactory;
import com.cornucopai.game.mcp.MockMcpClient;
import dev.langchain4j.mcp.McpToolProvider;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * T013 (spec 007 US1): the MCP tool provider is wired into every LangChain4j brain alongside the
 * base @Tool tools (SC-001, FR-003, R-046).
 */
class ToolboxWiringMcpTest {

  @Test
  void brainCarriesTheMcpToolProvider() {
    CardInfoTool cardTool =
        new CardInfoTool(new CardInfoService(Path.of("src/test/resources/cornucopia-fixture")));
    LlmDefaults defaults =
        new LlmDefaults("http://localhost:1235/v1", null, null, Duration.ofSeconds(5));
    LlmBrainFactory factory = new LlmBrainFactory("auto", defaults, List.of(cardTool));
    McpToolProvider provider =
        McpToolProviderFactory.createWithClient(
            McpIdeConfig.defaults(), new MockMcpClient("ide-mcp", List.of("read_file"), false));
    LlmBrain brain =
        factory.create(TestAgents.snapshot("agent-1"), "Joueur-1", "atelierX", List.of(), provider);
    assertThat(brain).isInstanceOf(LangChain4jBrain.class);
    LangChain4jBrain lcb = (LangChain4jBrain) brain;
    assertThat(lcb.tools()).contains(cardTool);
    assertThat(lcb.mcpToolProvider()).isSameAs(provider);
  }

  @Test
  void brainWithoutProviderStillWorks() {
    LlmDefaults defaults =
        new LlmDefaults("http://localhost:1235/v1", null, null, Duration.ofSeconds(5));
    LlmBrainFactory factory = new LlmBrainFactory("auto", defaults, List.of());
    LlmBrain brain =
        factory.create(TestAgents.snapshot("agent-1"), "Joueur-1", "atelierX", List.of());
    assertThat(((LangChain4jBrain) brain).mcpToolProvider()).isNull();
  }
}
