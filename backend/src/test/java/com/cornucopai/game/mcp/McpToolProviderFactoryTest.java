package com.cornucopai.game.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * T012 (spec 007 US1): the MCP tool provider whitelists exactly the configured tool names (FR-002,
 * R-046) and degrades gracefully when the server is down (FR-004).
 */
class McpToolProviderFactoryTest {

  private static final List<String> ALL_TEN =
      List.of(
          "get_project_dependencies",
          "get_project_modules",
          "list_directory_tree",
          "read_file",
          "search_file",
          "search_regex",
          "search_text",
          "delete_file",
          "write_file",
          "run_command"); // 3 decoys that must be filtered out

  @Test
  void providerExposesExactlyTheWhitelistedTools() {
    McpIdeConfig config = McpIdeConfig.defaults();
    MockMcpClient client = new MockMcpClient("ide-mcp", ALL_TEN, false);
    McpToolProvider provider = McpToolProviderFactory.createWithClient(config, client);
    Set<String> exposed = toolNames(provider);
    assertThat(exposed).containsExactlyInAnyOrderElementsOf(config.allowedToolNames());
    assertThat(exposed).doesNotContain("delete_file", "write_file", "run_command");
  }

  @Test
  void providerIsEmptyWhenServerFails() {
    McpIdeConfig config = McpIdeConfig.defaults();
    MockMcpClient failing = new MockMcpClient("ide-mcp", List.of(), true);
    McpToolProvider provider = McpToolProviderFactory.createWithClient(config, failing);
    assertThat(toolNames(provider)).isEmpty(); // no exception, FR-004
  }

  @Test
  void disabledConfigYieldsNullProvider() {
    McpIdeConfig config =
        McpIdeConfig.of(false, "http://127.0.0.1:64462/stream", "read_file", 30, false);
    assertThat(McpToolProviderFactory.create(config)).isNull();
  }

  @Test
  void mockConfigUsesMockClient() {
    McpIdeConfig config = McpIdeConfig.of(true, "http://unused", "read_file", 30, true);
    McpToolProvider provider = McpToolProviderFactory.create(config);
    assertThat(provider).isNotNull();
    assertThat(toolNames(provider)).containsExactly("read_file");
  }

  private static Set<String> toolNames(McpToolProvider provider) {
    ToolProviderResult result =
        provider.provideTools(
            new ToolProviderRequest(null, dev.langchain4j.data.message.UserMessage.from("test")));
    return result.tools().keySet().stream()
        .map(ToolSpecification::name)
        .collect(Collectors.toSet());
  }
}
