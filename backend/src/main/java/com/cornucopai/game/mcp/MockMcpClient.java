package com.cornucopai.game.mcp;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpGetPromptResult;
import dev.langchain4j.mcp.client.McpPrompt;
import dev.langchain4j.mcp.client.McpReadResourceResult;
import dev.langchain4j.mcp.client.McpResource;
import dev.langchain4j.mcp.client.McpResourceTemplate;
import dev.langchain4j.mcp.client.McpRoot;
import dev.langchain4j.service.tool.ToolExecutionResult;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * T015 (spec 007 US1): deterministic in-memory {@link McpClient} for CI (R-055). Lists scripted
 * tool names; executeTool returns a fixed string and counts calls; a failure mode makes listTools
 * throw (simulates the IDE MCP server being down).
 */
public class MockMcpClient implements McpClient {

  private final String key;
  private final List<String> toolNames;
  private final boolean failOnListTools;
  public final AtomicInteger executeToolCalls = new AtomicInteger();
  public String lastExecutedTool;

  public MockMcpClient(String key, List<String> toolNames, boolean failOnListTools) {
    this.key = key;
    this.toolNames = toolNames;
    this.failOnListTools = failOnListTools;
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public List<ToolSpecification> listTools() {
    if (failOnListTools) {
      throw new RuntimeException("MCP server unreachable (mock failure mode)");
    }
    return toolNames.stream()
        .map(n -> ToolSpecification.builder().name(n).description("mock tool " + n).build())
        .toList();
  }

  @Override
  public List<ToolSpecification> listTools(InvocationContext invocationContext) {
    return listTools();
  }

  @Override
  public ToolExecutionResult executeTool(ToolExecutionRequest request) {
    executeToolCalls.incrementAndGet();
    lastExecutedTool = request.name();
    return ToolExecutionResult.builder().resultText("mock result for " + request.name()).build();
  }

  @Override
  public ToolExecutionResult executeTool(ToolExecutionRequest request, InvocationContext ctx) {
    return executeTool(request);
  }

  @Override
  public List<McpResource> listResources() {
    return List.of();
  }

  @Override
  public List<McpResource> listResources(InvocationContext c) {
    return List.of();
  }

  @Override
  public List<McpResourceTemplate> listResourceTemplates() {
    return List.of();
  }

  @Override
  public List<McpResourceTemplate> listResourceTemplates(InvocationContext c) {
    return List.of();
  }

  @Override
  public McpReadResourceResult readResource(String uri) {
    throw new UnsupportedOperationException();
  }

  @Override
  public McpReadResourceResult readResource(String uri, InvocationContext c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void subscribeToResource(String uri) {}

  @Override
  public void unsubscribeFromResource(String uri) {}

  @Override
  public List<McpPrompt> listPrompts() {
    return List.of();
  }

  @Override
  public McpGetPromptResult getPrompt(String name, Map<String, Object> args) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void checkHealth() {
    if (failOnListTools) {
      throw new RuntimeException("unhealthy");
    }
  }

  @Override
  public void setRoots(List<McpRoot> roots) {}

  @Override
  public void close() {}
}
