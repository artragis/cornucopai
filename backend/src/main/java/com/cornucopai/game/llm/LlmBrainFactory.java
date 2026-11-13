package com.cornucopai.game.llm;

import com.cornucopai.game.card.CardInfoTool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.function.Function;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Builds the {@link LlmBrain} of one agent from its {@link AgentSnapshot} (spec 009, US3, FR-011):
 * {@code cornucopai.llm.brain=fake} → {@link FakeLlmBrain} (test profile, CI never calls a real
 * LLM, R-031); otherwise the single OpenAI-compatible {@link LangChain4jBrain} with the agent's
 * {@code openAiCompatibleUrl}/{@code modelName} and the API key resolved from the env var named by
 * {@code apiKeyVar} (FR-006/FR-013). The ACP provider path is removed (FR-011).
 *
 * <p>The system prompt is the base game prompt + the agent's {@code personalityPrompt} (FR-012,
 * {@link SystemPrompts#forPersonality}); the memory policy (id {@code <atelierId>/<agentName>},
 * window 40) is wired in {@link LangChain4jBrain}.
 */
@ApplicationScoped
public class LlmBrainFactory {

  /** Seam for env var lookup (FR-013) — default {@code System::getenv}, replaced in tests. */
  public interface EnvResolver extends Function<String, String> {}

  private final String mode;
  private final LlmDefaults defaults;
  private final EnvResolver envResolver;
  private volatile List<Object> tools;

  @Inject
  public LlmBrainFactory(
      @ConfigProperty(name = "cornucopai.llm.brain", defaultValue = "auto") String mode,
      LlmDefaults defaults) {
    this(mode, defaults, List.of(), (EnvResolver) System::getenv);
  }

  /** Test-friendly constructor with an explicit toolbox. */
  public LlmBrainFactory(String mode, LlmDefaults defaults, List<Object> tools) {
    this(mode, defaults, tools, (EnvResolver) System::getenv);
  }

  /** Test-friendly constructor with an explicit toolbox and env resolver. */
  public LlmBrainFactory(
      String mode, LlmDefaults defaults, List<Object> tools, EnvResolver envResolver) {
    this.mode = mode;
    this.defaults = defaults;
    this.tools = tools == null ? List.of() : List.copyOf(tools);
    this.envResolver = envResolver;
  }

  /** CDI wiring of the toolbox (US3, FR-005): every LangChain4j brain gets the CardInfoTool. */
  @Inject
  void setCardInfoTool(CardInfoTool cardInfoTool) {
    this.tools = List.of(cardInfoTool);
  }

  /**
   * FR-006/FR-013: {@code apiKeyVar} empty → no key; otherwise the key is {@code
   * envResolver.apply(apiKeyVar)} and a missing variable is an error naming it.
   */
  static String resolveApiKey(AgentSnapshot snapshot, EnvResolver envResolver) {
    String var = snapshot.apiKeyVar();
    if (var == null || var.isBlank()) {
      return null; // no key required (FR-006)
    }
    String key = envResolver.apply(var);
    if (key == null) {
      throw new IllegalArgumentException(
          "environment variable not set: "
              + var
              + " (apiKeyVar of agent "
              + snapshot.agentName()
              + ")");
    }
    return key;
  }

  public LlmBrain create(AgentSnapshot snapshot, String agentName, String atelierId) {
    return create(snapshot, agentName, atelierId, List.of(), null);
  }

  /** Creates an LLM brain with per-game tools appended to the base toolbox. */
  public LlmBrain create(
      AgentSnapshot snapshot, String agentName, String atelierId, List<Object> gameTools) {
    return create(snapshot, agentName, atelierId, gameTools, null);
  }

  /**
   * Creates an LLM brain with per-game tools appended to the base toolbox.
   *
   * @param gameTools per-game tools (spec 005 US-2: the {@code TourInfoTool}) appended to the base
   *     toolbox (R-039)
   * @param mcpToolProvider spec 007 US1 (R-046): the IDE MCP tool provider, wired via {@code
   *     .toolProvider(...)} in addition to {@code .tools(...)}; null = no MCP tools
   */
  public LlmBrain create(
      AgentSnapshot snapshot,
      String agentName,
      String atelierId,
      List<Object> gameTools,
      dev.langchain4j.service.tool.ToolProvider mcpToolProvider) {
    // FR-013: the apiKeyVar env check runs in EVERY mode (even fake), so a missing variable
    // always refuses the launch with a message naming it
    String apiKey = resolveApiKey(snapshot, envResolver);
    if ("fake".equalsIgnoreCase(mode)) {
      return new FakeLlmBrain();
    }
    List<Object> allTools = new java.util.ArrayList<>(tools);
    if (gameTools != null) {
      allTools.addAll(gameTools);
    }
    // FR-011: single LLM implementation, OpenAI-compatible endpoint from the agent snapshot
    return new LangChain4jBrain(
        snapshot,
        apiKey,
        agentName,
        atelierId,
        defaults,
        SystemPrompts.forPersonality(snapshot.personalityPrompt()),
        allTools,
        mcpToolProvider);
  }
}
