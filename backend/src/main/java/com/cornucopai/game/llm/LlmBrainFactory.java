package com.cornucopai.game.llm;

import com.cornucopai.game.card.CardInfoTool;
import com.cornucopai.game.model.Archetype;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

/**
 * Builds the {@link LlmBrain} of one agent from its {@link AgentLlmConfig}
 * (R-029): {@code cornucopai.llm.brain=fake} → {@link FakeLlmBrain} (test profile, CI
 * never calls a real LLM, R-031); otherwise OPENAI_COMPATIBLE → {@link LangChain4jBrain}
 * (toolbox + memory), ACP → {@link AcpCliBrain} (one-shot CLI).
 *
 * <p>US4 (T039/T040): the per-archetype system prompt
 * ({@code SystemPrompts.forArchetype}) and the memory policy (id
 * {@code <atelierId>/<agentName>}, window 40) are wired here and in
 * {@link LangChain4jBrain}.
 */
@ApplicationScoped
public class LlmBrainFactory {

    /** US4 (T039/T040): per-archetype system prompt, wired here. */
    static String systemPromptFor(Archetype archetype) {
        return SystemPrompts.forArchetype(archetype);
    }

    private final String mode;
    private final LlmDefaults defaults;
    private volatile List<Object> tools;

    @Inject
    public LlmBrainFactory(@ConfigProperty(name = "cornucopai.llm.brain", defaultValue = "auto") String mode,
                           LlmDefaults defaults) {
        this(mode, defaults, List.of());
    }

    /** Test-friendly constructor with an explicit toolbox. */
    public LlmBrainFactory(String mode, LlmDefaults defaults, List<Object> tools) {
        this.mode = mode;
        this.defaults = defaults;
        this.tools = tools == null ? List.of() : List.copyOf(tools);
    }

    /** CDI wiring of the toolbox (US3, FR-005): every LangChain4j brain gets the CardInfoTool. */
    @Inject
    void setCardInfoTool(CardInfoTool cardInfoTool) {
        this.tools = List.of(cardInfoTool);
    }

    public LlmBrain create(AgentLlmConfig config, String agentName, Archetype archetype, String atelierId) {
        if ("fake".equalsIgnoreCase(mode)) {
            return new FakeLlmBrain();
        }
        return switch (config.providerType()) {
            case OPENAI_COMPATIBLE ->
                    new LangChain4jBrain(config, agentName, atelierId, defaults, systemPromptFor(archetype), tools);
            case ACP -> new AcpCliBrain(config, agentName, systemPromptFor(archetype));
        };
    }
}
