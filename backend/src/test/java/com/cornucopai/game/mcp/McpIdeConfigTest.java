package com.cornucopai.game.mcp;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class McpIdeConfigTest {

    @Test
    void defaultsMatchSpec007() {
        McpIdeConfig config = McpIdeConfig.defaults();
        assertThat(config.enabled()).isTrue();
        assertThat(config.url()).isEqualTo("http://127.0.0.1:64462/stream");
        assertThat(config.allowedToolNames()).containsExactlyInAnyOrder(
                "get_project_dependencies", "get_project_modules", "list_directory_tree",
                "read_file", "search_file", "search_regex", "search_text");
        assertThat(config.timeoutSeconds()).isEqualTo(30);
        assertThat(config.mock()).isFalse();
    }

    @Test
    void parsesCommaSeparatedToolList() {
        McpIdeConfig config = McpIdeConfig.of(true, "http://x/stream", "a, b ,c", 10, false);
        assertThat(config.allowedToolNames()).isEqualTo(Set.of("a", "b", "c"));
    }

    @Test
    void memoryWindowDefault() {
        assertThat(McpIdeConfig.DEFAULT_MEMORY_PROMPT_WINDOW).isEqualTo(20);
    }

    @Test
    void autoHealthCheckDefaultsToFalseAndProtocolVersionToAuto() {
        // observed 2026-11-11: the default health-checker spams WARN reconnection errors
        // when the IDE MCP server is down — must default to OFF
        McpIdeConfig config = McpIdeConfig.defaults();
        assertThat(config.autoHealthCheck()).isFalse();
        assertThat(config.protocolVersion()).isBlank(); // blank = auto-detect
    }
}
