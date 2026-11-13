package com.cornucopai.game.llm;

import com.cornucopai.game.llm.dto.AnswerDto;
import com.cornucopai.game.llm.dto.PlayedCardDto;
import com.cornucopai.game.llm.dto.ReactionDto;
import com.cornucopai.game.llm.dto.VoteDto;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * {@link LlmBrain} backed by a per-agent LangChain4j {@code AiServices} proxy (R-021): an {@link
 * OpenAiChatModel} built from the agent's {@link AgentSnapshot} (spec 009: {@code
 * openAiCompatibleUrl} as base URL, {@code modelName}, resolved API key — null means NO
 * Authorization header, verified in DefaultOpenAiClient — timeout from the server defaults), a
 * {@link MessageWindowChatMemory} private to this agent, the system prompt (base rules + the
 * agent's {@code personalityPrompt}, FR-012), and the toolbox ({@code CardInfoTool}).
 *
 * <p>Memory policy: id = {@code <atelierId>/<agentName>}, window 40 (R-025).
 */
public class LangChain4jBrain implements LlmBrain {

  private static final Logger LOG = Logger.getLogger(LangChain4jBrain.class);

  private final AgentSnapshot snapshot;
  private final String apiKey;
  private final String agentName;
  private final String atelierId;
  private final LlmDefaults defaults;
  private final ChatMemory chatMemory;
  private final String systemPrompt;
  private final List<Object> tools;

  /**
   * Spec 007 US1 (R-046): IDE MCP tool provider, wired via .toolProvider(...); null = no MCP tools.
   */
  private final dev.langchain4j.service.tool.ToolProvider mcpToolProvider;

  /**
   * Built lazily on first call: AiServices touches the CDI Arc container, which is absent in plain
   * unit tests.
   */
  private volatile PlayerAiService ai;

  /** Spec 005 US-3 (R-040): the vote call is capped at 100 output tokens via a dedicated model. */
  public static final int VOTE_MAX_TOKENS = 100;

  private volatile PlayerAiService voteAi;

  public LangChain4jBrain(
      AgentSnapshot snapshot,
      String apiKey,
      String agentName,
      String atelierId,
      LlmDefaults defaults,
      String systemPrompt,
      List<Object> tools) {
    this(snapshot, apiKey, agentName, atelierId, defaults, systemPrompt, tools, null);
  }

  public LangChain4jBrain(
      AgentSnapshot snapshot,
      String apiKey,
      String agentName,
      String atelierId,
      LlmDefaults defaults,
      String systemPrompt,
      List<Object> tools,
      dev.langchain4j.service.tool.ToolProvider mcpToolProvider) {
    this.snapshot = snapshot;
    this.apiKey = apiKey;
    this.agentName = agentName;
    this.atelierId = atelierId;
    this.defaults = defaults;
    this.chatMemory =
        MessageWindowChatMemory.builder()
            .id(atelierId + "/" + agentName) // per agent, per atelier (FR-010, R-025)
            .maxMessages(40)
            .build();
    this.systemPrompt = systemPrompt;
    this.tools = List.copyOf(tools);
    this.mcpToolProvider = mcpToolProvider;
  }

  private OpenAiChatModel.OpenAiChatModelBuilder modelBuilder() {
    OpenAiChatModel.OpenAiChatModelBuilder modelBuilder =
        OpenAiChatModel.builder()
            .baseUrl(snapshot.openAiCompatibleUrl()) // spec 009: per-agent endpoint (FR-011)
            .modelName(
                snapshot.modelName() == null || snapshot.modelName().isBlank()
                    ? "default"
                    : snapshot.modelName())
            .timeout(defaults.timeout())
            .maxRetries(1)
            // observed 2026-11-11: some servers (LM Studio/gemma) return message content as an
            // array of parts, which langchain4j-open-ai 1.23.0 cannot deserialize — sanitize it
            .httpClientBuilder(new SanitizingHttpClientBuilder());
    // FR-006: the key was resolved from the apiKeyVar env var by LlmBrainFactory; null = no auth
    if (apiKey != null && !apiKey.isBlank()) {
      modelBuilder.apiKey(apiKey);
    }
    return modelBuilder;
  }

  /**
   * Observed 2026-11-11 on a real run: the model called {@code get_tour_info} / MCP tools from the
   * VOTE prompt, but the vote proxy had no tools registered, so langchain4j's default {@code
   * HallucinatedToolNameStrategy.THROW_EXCEPTION} failed the whole vote call. Two fixes: (1) every
   * proxy (main AND vote) gets the full toolbox + MCP provider — the vote prompt's branch (b)
   * requires factual MCP verification (R-061); (2) a lenient hallucinated-tool strategy returns an
   * explanatory tool result instead of throwing, so one bad tool name never kills the call
   * (non-blocking fallback, spec 004 FR-013).
   */
  static final java.util.function.Function<
          dev.langchain4j.agent.tool.ToolExecutionRequest,
          dev.langchain4j.data.message.ToolExecutionResultMessage>
      LENIENT_HALLUCINATED_TOOL =
          request ->
              dev.langchain4j.data.message.ToolExecutionResultMessage.from(
                  request,
                  "L'outil '"
                      + request.name()
                      + "' n'existe pas dans ta toolbox. N'invente pas d'outil : utilise "
                      + "uniquement ceux réellement disponibles (get_tour_info, get_project_facts, "
                      + "add_project_fact, et les outils MCP), ou réponds sans outil.");

  /** Common AiServices builder: model, system prompt, memory, toolbox, MCP provider, limits. */
  private AiServices<PlayerAiService> baseAiServiceBuilder() {
    var builder =
        AiServices.builder(PlayerAiService.class)
            .chatModel(modelBuilder().build())
            .systemMessageProvider(id -> systemPrompt)
            .chatMemory(chatMemory)
            // observed 2026-11-11: agents chain many MCP calls before answering
            .maxToolCallingRoundTrips(100)
            .hallucinatedToolNameStrategy(LENIENT_HALLUCINATED_TOOL)
            .tools(tools);
    if (mcpToolProvider != null) {
      // spec 007 US1 (R-046): MCP tools alongside @Tool tools
      builder.toolProvider(mcpToolProvider);
    }
    return builder;
  }

  private PlayerAiService ai() {
    PlayerAiService local = ai;
    if (local == null) {
      synchronized (this) {
        local = ai;
        if (local == null) {
          local = baseAiServiceBuilder().build();
          ai = local;
        }
      }
    }
    return local;
  }

  /**
   * Dedicated vote proxy (R-040): same endpoint/memory/system prompt and SAME toolbox as the main
   * proxy — since R-061 the vote prompt's branch (b) orders a factual MCP verification, so the vote
   * path must have the tools (observed 2026-11-11: without them every vote failed with "no such
   * tool exists"). Output stays the bare decision-tree JSON.
   */
  private PlayerAiService voteAi() {
    PlayerAiService local = voteAi;
    if (local == null) {
      synchronized (this) {
        local = voteAi;
        if (local == null) {
          local = baseAiServiceBuilder().build();
          voteAi = local;
        }
      }
    }
    return local;
  }

  public int maxCompletionTokens() {
    return VOTE_MAX_TOKENS;
  }

  public boolean hasDedicatedVoteModel() {
    return true;
  }

  @Override
  public PlayedCardDto playCard(String userPrompt) {
    return call("playCard", () -> ai().playCard(userPrompt));
  }

  @Override
  public ReactionDto react(String userPrompt) {
    return call("react", () -> ai().react(userPrompt));
  }

  @Override
  public AnswerDto answerQuestion(String userPrompt) {
    return call("answerQuestion", () -> ai().answerQuestion(userPrompt));
  }

  @Override
  public VoteDto vote(String userPrompt) {
    return call("vote", () -> voteAi().vote(userPrompt));
  }

  @Override
  public String debate(String userPrompt) {
    return call("debate", () -> ai().debate(userPrompt));
  }

  /**
   * Structured logging of every LLM call (constitution IV): agent, method, duration. The prompt
   * content and the apiKey are NEVER logged (security-sensitive).
   */
  private <T> T call(String what, java.util.function.Supplier<T> fn) {
    long start = System.nanoTime();
    try {
      T result = fn.get();
      LOG.infof(
          "LLM call agent=%s what=%s durationMs=%d ok=true",
          agentName, what, (System.nanoTime() - start) / 1_000_000);
      return result;
    } catch (Exception e) {
      LOG.warnf(
          e,
          "LLM call agent=%s what=%s durationMs=%d ok=false",
          agentName,
          what,
          (System.nanoTime() - start) / 1_000_000);
      throw e;
    }
  }

  /** Test accessors (US4 memory-isolation and US3 toolbox-wiring tests). */
  public ChatMemory chatMemory() {
    return chatMemory;
  }

  public String systemPrompt() {
    return systemPrompt;
  }

  public List<Object> tools() {
    return tools;
  }

  /** Spec 007 US1 test accessor (T013, SC-001). */
  public dev.langchain4j.service.tool.ToolProvider mcpToolProvider() {
    return mcpToolProvider;
  }

  /** Spec 009 (US3) test accessors: the brain is built from the agent snapshot + resolved key. */
  public AgentSnapshot snapshot() {
    return snapshot;
  }

  /** The API key resolved from the apiKeyVar env var (null = no auth, FR-006). */
  public String apiKey() {
    return apiKey;
  }
}
