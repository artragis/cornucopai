package com.cornucopai.game.agent;

import com.cornucopai.game.PlayerAgent;
import com.cornucopai.game.llm.AgentLlmConfig;
import com.cornucopai.game.llm.LegContext;
import com.cornucopai.game.llm.LlmBrain;
import com.cornucopai.game.llm.LlmBrainFactory;
import com.cornucopai.game.llm.TourInfoTool;
import com.cornucopai.game.model.Participant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates the player agents for a game. Spec 004 (FR-003): games are played by the single LLM
 * implementation {@link LlmPlayerAgent}, one per participant, each with its own {@link LlmBrain}
 * built from its {@link AgentLlmConfig}. The deterministic archetype agents ({@code GrumpyAgent},
 * {@code HappyAgent}, {@code GreedyAgent}, {@code ProfAgent}) are kept in this package for UNIT
 * TESTS ONLY and are no longer returned here.
 */
@ApplicationScoped
public class AgentFactory {

  private final LlmBrainFactory llmBrainFactory;
  private final com.cornucopai.game.mcp.McpToolProviderFactory mcpToolProviderFactory;
  private final dev.langchain4j.service.tool.ToolProvider fixedMcpToolProvider;

  @Inject
  public AgentFactory(
      LlmBrainFactory llmBrainFactory,
      com.cornucopai.game.mcp.McpToolProviderFactory mcpToolProviderFactory) {
    this.llmBrainFactory = llmBrainFactory;
    this.mcpToolProviderFactory = mcpToolProviderFactory;
    this.fixedMcpToolProvider = null;
  }

  /** Test-friendly constructor without MCP (spec 004 path). */
  public AgentFactory(LlmBrainFactory llmBrainFactory) {
    this(llmBrainFactory, (com.cornucopai.game.mcp.McpToolProviderFactory) null);
  }

  /** Test-friendly constructor with an explicit MCP provider (spec 007 US1, T014). */
  public AgentFactory(
      LlmBrainFactory llmBrainFactory, dev.langchain4j.service.tool.ToolProvider mcpToolProvider) {
    this.llmBrainFactory = llmBrainFactory;
    this.mcpToolProviderFactory = null;
    this.fixedMcpToolProvider = mcpToolProvider;
  }

  /**
   * One {@link LlmPlayerAgent} per participant, index-aligned with {@code configs}; {@code
   * atelierId} scopes the per-agent memory (R-025). Spec 007 US1 (R-046): one MCP tool provider per
   * atelier, shared by all its agents.
   */
  public List<PlayerAgent> createAgents(
      List<Participant> players, List<AgentLlmConfig> configs, String atelierId) {
    if (players.size() != configs.size()) {
      throw new IllegalArgumentException("players and configs must be index-aligned");
    }
    // spec 005 US-2 (R-038/R-039): one LegContext + one TourInfoTool per game, shared by all its
    // agents
    LegContext legContext = new LegContext(atelierId);
    TourInfoTool tourInfoTool = new TourInfoTool(legContext);
    // spec 007 US2 (R-049): one SharedProjectMemory per atelier, shared by all agents;
    // one ProjectFactsTool PER agent (so the fact author is the calling agent) over that shared
    // memory
    com.cornucopai.game.memory.SharedProjectMemory sharedMemory =
        new com.cornucopai.game.memory.SharedProjectMemory(atelierId);
    // spec 007 US1: build (or reuse) the IDE MCP tool provider once per atelier
    dev.langchain4j.service.tool.ToolProvider mcpProvider =
        fixedMcpToolProvider != null
            ? fixedMcpToolProvider
            : (mcpToolProviderFactory != null ? mcpToolProviderFactory.create() : null);
    List<PlayerAgent> agents = new ArrayList<>(players.size());
    for (int i = 0; i < players.size(); i++) {
      Participant p = players.get(i);
      com.cornucopai.game.memory.ProjectFactsTool factsTool =
          new com.cornucopai.game.memory.ProjectFactsTool(
              sharedMemory,
              p::name,
              () -> {
                com.cornucopai.game.model.Leg l = legContext.current();
                return l == null ? 0 : l.turnIndex();
              });
      List<Object> gameTools = List.of(tourInfoTool, factsTool);
      LlmBrain brain =
          llmBrainFactory.create(
              configs.get(i), p.name(), p.archetype(), atelierId, gameTools, mcpProvider);
      agents.add(new LlmPlayerAgent(p.name(), p.archetype(), brain, legContext, sharedMemory));
    }
    return agents;
  }
}
