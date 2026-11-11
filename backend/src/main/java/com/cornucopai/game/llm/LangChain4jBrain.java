package com.cornucopai.game.llm;

import com.cornucopai.game.llm.dto.AnswerDto;
import com.cornucopai.game.llm.dto.PlayedCardDto;
import com.cornucopai.game.llm.dto.ReactionDto;
import com.cornucopai.game.llm.dto.VoteDto;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * {@link LlmBrain} backed by a per-agent LangChain4j {@code AiServices} proxy
 * (R-021): an {@link OpenAiChatModel} built from the agent's own
 * {@link AgentLlmConfig} (custom base URL, optional API key — null means NO
 * Authorization header, verified in DefaultOpenAiClient — model name, timeout),
 * a {@link MessageWindowChatMemory} private to this agent, the system prompt, and
 * the toolbox ({@code CardInfoTool} from US3).
 *
 * <p>Memory policy (id = {@code <atelierId>/<agentName>}, window 40) and the
 * per-archetype system prompt are finalized by US4 (T039/T040); until then a
 * placeholder prompt and a smaller window are used.
 */
public class LangChain4jBrain implements LlmBrain {

    private static final Logger LOG = Logger.getLogger(LangChain4jBrain.class);

    private final AgentLlmConfig config;
    private final String agentName;
    private final String atelierId;
    private final LlmDefaults defaults;
    private final ChatMemory chatMemory;
    private final String systemPrompt;
    private final List<Object> tools;
    /** Spec 007 US1 (R-046): IDE MCP tool provider, wired via .toolProvider(...); null = no MCP tools. */
    private final dev.langchain4j.service.tool.ToolProvider mcpToolProvider;
    /** Built lazily on first call: AiServices touches the CDI Arc container, which is absent in plain unit tests. */
    private volatile PlayerAiService ai;
    /** Spec 005 US-3 (R-040): the vote call is capped at 100 output tokens via a dedicated model. */
    public static final int VOTE_MAX_TOKENS = 100;
    private volatile PlayerAiService voteAi;

    public LangChain4jBrain(AgentLlmConfig config, String agentName, String atelierId,
                            LlmDefaults defaults, String systemPrompt, List<Object> tools) {
        this(config, agentName, atelierId, defaults, systemPrompt, tools, null);
    }

    public LangChain4jBrain(AgentLlmConfig config, String agentName, String atelierId,
                            LlmDefaults defaults, String systemPrompt, List<Object> tools,
                            dev.langchain4j.service.tool.ToolProvider mcpToolProvider) {
        this.config = config;
        this.agentName = agentName;
        this.atelierId = atelierId;
        this.defaults = defaults;
        this.chatMemory = MessageWindowChatMemory.builder()
                .id(atelierId + "/" + agentName) // per agent, per atelier (FR-010, R-025)
                .maxMessages(40)
                .build();
        this.systemPrompt = systemPrompt;
        this.tools = List.copyOf(tools);
        this.mcpToolProvider = mcpToolProvider;
    }

    private OpenAiChatModel.OpenAiChatModelBuilder modelBuilder() {
        OpenAiChatModel.OpenAiChatModelBuilder modelBuilder = OpenAiChatModel.builder()
                .baseUrl(config.baseUrl())
                .modelName(config.modelName() == null || config.modelName().isBlank() ? "default" : config.modelName())
                .timeout(defaults.timeout())
                .maxRetries(1);
        if (config.apiKey() != null && !config.apiKey().isBlank()) {
            modelBuilder.apiKey(config.apiKey());
        }
        return modelBuilder;
    }

    private PlayerAiService ai() {
        PlayerAiService local = ai;
        if (local == null) {
            synchronized (this) {
                local = ai;
                if (local == null) {
                    var builder = AiServices.builder(PlayerAiService.class)
                            .chatModel(modelBuilder().build())
                            .systemMessageProvider(id -> systemPrompt)
                            .chatMemory(chatMemory)
                            .tools(tools);
                    if (mcpToolProvider != null) {
                        builder.toolProvider(mcpToolProvider); // spec 007 US1 (R-046): MCP tools alongside @Tool tools
                    }
                    local = builder.build();
                    ai = local;
                }
            }
        }
        return local;
    }

    /**
     * Dedicated vote proxy (R-040): same endpoint/memory/system prompt, but the model is
     * built with {@code maxCompletionTokens(VOTE_MAX_TOKENS)} and NO tools — the vote must be a bare
     * YES/NO JSON, never reasoning.
     */
    private PlayerAiService voteAi() {
        PlayerAiService local = voteAi;
        if (local == null) {
            synchronized (this) {
                local = voteAi;
                if (local == null) {
                    local = AiServices.builder(PlayerAiService.class)
                            .chatModel(modelBuilder().build())
                            .systemMessageProvider(id -> systemPrompt)
                            .chatMemory(chatMemory)
                            .build();
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
     * Structured logging of every LLM call (constitution IV): agent, method, duration.
     * The prompt content and the apiKey are NEVER logged (security-sensitive).
     */
    private <T> T call(String what, java.util.function.Supplier<T> fn) {
        long start = System.nanoTime();
        try {
            T result = fn.get();
            LOG.infof("LLM call agent=%s what=%s durationMs=%d ok=true", agentName, what,
                    (System.nanoTime() - start) / 1_000_000);
            return result;
        } catch (Exception e) {
            LOG.warnf(e, "LLM call agent=%s what=%s durationMs=%d ok=false", agentName, what,
                    (System.nanoTime() - start) / 1_000_000);
            throw e;
        }
    }

    /** Test accessors (US4 memory-isolation and US3 toolbox-wiring tests). */
    public ChatMemory chatMemory() { return chatMemory; }
    public String systemPrompt() { return systemPrompt; }
    public List<Object> tools() { return tools; }
    /** Spec 007 US1 test accessor (T013, SC-001). */
    public dev.langchain4j.service.tool.ToolProvider mcpToolProvider() { return mcpToolProvider; }
}
