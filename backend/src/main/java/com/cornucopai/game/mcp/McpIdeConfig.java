package com.cornucopai.game.mcp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Configuration of the IDE MCP server (spec 007, R-048, FR-001/FR-002/FR-014). All values come from
 * MicroProfile config properties with defaults matching the test setup — no code change needed to
 * point at another MCP server.
 *
 * <p>Clarification 2026-11-11: the MCP server always serves the project currently open in the
 * user's IDE; CornucopAI does NOT configure a project path.
 */
@ApplicationScoped
public class McpIdeConfig {

  public static final int DEFAULT_MEMORY_PROMPT_WINDOW = 20;

  private final boolean enabled;
  private final String url;
  private final Set<String> allowedToolNames;
  private final int timeoutSeconds;
  private final boolean mock;

  /** MCP protocol version to skip auto-detection (e.g. "2025-11-25"); null/blank = auto-detect. */
  private final String protocolVersion;

  /**
   * Auto health-check / reconnection loop of DefaultMcpClient; default FALSE to avoid log spam when
   * the server is down.
   */
  private final boolean autoHealthCheck;

  @Inject
  public McpIdeConfig(
      @ConfigProperty(name = "cornucopai.mcp.ide.enabled", defaultValue = "true") boolean enabled,
      @ConfigProperty(
              name = "cornucopai.mcp.ide.url",
              defaultValue = "http://127.0.0.1:64462/stream")
          String url,
      @ConfigProperty(
              name = "cornucopai.mcp.ide.tools",
              defaultValue =
                  "get_project_dependencies,get_project_modules,list_directory_tree,read_file,"
                      + "search_file,search_regex,search_text")
          String tools,
      @ConfigProperty(name = "cornucopai.mcp.ide.timeout-seconds", defaultValue = "30")
          int timeoutSeconds,
      @ConfigProperty(name = "cornucopai.mcp.ide.mock", defaultValue = "false") boolean mock,
      @ConfigProperty(name = "cornucopai.mcp.ide.protocol-version")
          java.util.Optional<String> protocolVersion,
      @ConfigProperty(name = "cornucopai.mcp.ide.auto-health-check", defaultValue = "false")
          boolean autoHealthCheck) {
    this.enabled = enabled;
    this.url = url;
    this.allowedToolNames = parseTools(tools);
    this.timeoutSeconds = timeoutSeconds;
    this.mock = mock;
    this.protocolVersion = protocolVersion.orElse("");
    this.autoHealthCheck = autoHealthCheck;
  }

  private McpIdeConfig(
      boolean enabled,
      String url,
      Set<String> allowedToolNames,
      int timeoutSeconds,
      boolean mock,
      String protocolVersion,
      boolean autoHealthCheck) {
    this.enabled = enabled;
    this.url = url;
    this.allowedToolNames = allowedToolNames;
    this.timeoutSeconds = timeoutSeconds;
    this.mock = mock;
    this.protocolVersion = protocolVersion;
    this.autoHealthCheck = autoHealthCheck;
  }

  private static Set<String> parseTools(String csv) {
    if (csv == null || csv.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
  }

  /** Test-friendly factory with spec-007 defaults. */
  public static McpIdeConfig defaults() {
    return new McpIdeConfig(
        true,
        "http://127.0.0.1:64462/stream",
        parseTools(
            "get_project_dependencies,get_project_modules,list_directory_tree,read_file,"
                + "search_file,search_regex,search_text"),
        30,
        false,
        "",
        false);
  }

  /** Test-friendly factory from raw values. */
  public static McpIdeConfig of(
      boolean enabled, String url, String toolsCsv, int timeoutSeconds, boolean mock) {
    return new McpIdeConfig(enabled, url, parseTools(toolsCsv), timeoutSeconds, mock, "", false);
  }

  public boolean enabled() {
    return enabled;
  }

  public String url() {
    return url;
  }

  public Set<String> allowedToolNames() {
    return allowedToolNames;
  }

  public int timeoutSeconds() {
    return timeoutSeconds;
  }

  public boolean mock() {
    return mock;
  }

  public String protocolVersion() {
    return protocolVersion;
  }

  public boolean autoHealthCheck() {
    return autoHealthCheck;
  }
}
