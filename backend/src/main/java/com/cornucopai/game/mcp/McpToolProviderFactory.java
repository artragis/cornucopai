package com.cornucopai.game.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the {@link McpToolProvider} that exposes the IDE MCP server's tools to the
 * player agents (spec 007 US1, R-046/R-047, FR-001..FR-004).
 *
 * <p>Wiring (verified against langchain4j-mcp 1.23.0-beta33):
 * {@code StreamableHttpMcpTransport} → {@code DefaultMcpClient} (tool list cached)
 * → {@code McpToolProvider} with {@code filterToolNames(...)} as the whitelist and
 * {@code failIfOneServerFails(false)} so a down server never blocks a game.
 *
 * <p>Failure policy (FR-004, R-047): any exception while building the client/provider
 * is caught and results in an empty-tool provider (plus a WARN log) — the game always
 * starts. When {@code cornucopai.mcp.ide.enabled=false}, {@link #create} returns null
 * and brains simply get no MCP tools. When {@code cornucopai.mcp.ide.mock=true} (test
 * profile, R-055), a {@link MockMcpClient} is used instead of a real HTTP connection.
 *
 * <p>Security note (clarification 2026-11-11, R-054): the MCP server is expected to be
 * a LOCAL IDE server (127.0.0.1) whose results are trusted and NOT wrapped in a
 * DONNÉES anti-injection section. If the URL is ever changed to a non-localhost
 * address, authentication and result encapsulation MUST be added.
 */
@ApplicationScoped
public class McpToolProviderFactory {

    private static final Logger LOG = Logger.getLogger(McpToolProviderFactory.class);

    private final McpIdeConfig config;

    @Inject
    public McpToolProviderFactory(McpIdeConfig config) {
        this.config = config;
    }

    /**
     * Builds the provider for one atelier. Returns {@code null} when MCP is disabled.
     * Never throws — a down/unreachable server yields an empty-tool provider (FR-004).
     */
    public McpToolProvider create() {
        return create(config);
    }

    /** Static variant (test seam, T012). */
    public static McpToolProvider create(McpIdeConfig config) {
        if (!config.enabled()) {
            LOG.info("MCP IDE disabled (cornucopai.mcp.ide.enabled=false) — no MCP tools");
            return null;
        }
        if (config.mock()) {
            // test profile (R-055): deterministic mock client listing the whitelisted tools
            MockMcpClient mock = new MockMcpClient("ide-mcp",
                    new ArrayList<>(config.allowedToolNames()), false);
            return buildProvider(config, mock);
        }
        try {
            McpTransport transport = StreamableHttpMcpTransport.builder()
                    .url(config.url())
                    .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                    .build();
            DefaultMcpClient.Builder clientBuilder = DefaultMcpClient.builder()
                    .key("ide-mcp")
                    .transport(transport)
                    .cacheToolList(true)
                    .toolExecutionTimeout(Duration.ofSeconds(config.timeoutSeconds()))
                    // Avoid the background reconnection loop spamming WARN logs when the
                    // IDE server is down (observed 2026-11-11): no auto health check.
                    .autoHealthCheck(config.autoHealthCheck());
            // Skip protocol auto-detection when a version is pinned: some servers reject
            // the unknown-method probe the detection sends (observed 2026-11-11).
            if (config.protocolVersion() != null && !config.protocolVersion().isBlank()) {
                clientBuilder.protocolVersion(config.protocolVersion().trim());
            }
            McpClient client = clientBuilder.build();
            return buildProvider(config, client);
        } catch (Exception e) {
            LOG.warnf(e, "MCP IDE server unreachable at %s — continuing with zero MCP tools (FR-004)", config.url());
            return emptyProvider();
        }
    }

    /** Test seam: build a provider from an explicit client (mock or real). */
    public static McpToolProvider createWithClient(McpIdeConfig config, McpClient client) {
        if (!config.enabled()) {
            return null;
        }
        try {
            return buildProvider(config, client);
        } catch (Exception e) {
            LOG.warnf(e, "MCP client failed — empty provider (FR-004)");
            return emptyProvider();
        }
    }

    private static McpToolProvider buildProvider(McpIdeConfig config, McpClient client) {
        McpToolProvider provider = McpToolProvider.builder()
                .mcpClients(client)
                .failIfOneServerFails(false)
                .filterToolNames(new ArrayList<>(config.allowedToolNames()))
                .build();
        try {
            List<String> exposed = client.listTools().stream()
                    .map(t -> t.name())
                    .filter(config.allowedToolNames()::contains)
                    .toList();
            LOG.infof("MCP IDE tools exposed to agents: %s", exposed);
        } catch (Exception e) {
            LOG.warnf(e, "Could not list MCP tools (server down?) — provider will be empty");
        }
        return provider;
    }

    private static McpToolProvider emptyProvider() {
        return McpToolProvider.builder()
                .mcpClients(List.<McpClient>of())
                .failIfOneServerFails(false)
                .build();
    }
}
