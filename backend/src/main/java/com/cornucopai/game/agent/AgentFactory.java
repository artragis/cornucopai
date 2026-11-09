package com.cornucopai.game.agent;

import com.cornucopai.game.PlayerAgent;
import com.cornucopai.game.llm.AgentLlmConfig;
import com.cornucopai.game.llm.LlmBrain;
import com.cornucopai.game.llm.LlmBrainFactory;
import com.cornucopai.game.model.Participant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates the player agents for a game. Spec 004 (FR-003): games are played by the
 * single LLM implementation {@link LlmPlayerAgent}, one per participant, each with
 * its own {@link LlmBrain} built from its {@link AgentLlmConfig}. The deterministic
 * archetype agents ({@code GrumpyAgent}, {@code HappyAgent}, {@code GreedyAgent},
 * {@code ProfAgent}) are kept in this package for UNIT TESTS ONLY and are no longer
 * returned here.
 */
@ApplicationScoped
public class AgentFactory {

    private final LlmBrainFactory llmBrainFactory;

    @Inject
    public AgentFactory(LlmBrainFactory llmBrainFactory) {
        this.llmBrainFactory = llmBrainFactory;
    }

    /**
     * One {@link LlmPlayerAgent} per participant, index-aligned with
     * {@code configs}; {@code atelierId} scopes the per-agent memory (R-025).
     */
    public List<PlayerAgent> createAgents(List<Participant> players, List<AgentLlmConfig> configs, String atelierId) {
        if (players.size() != configs.size()) {
            throw new IllegalArgumentException("players and configs must be index-aligned");
        }
        List<PlayerAgent> agents = new ArrayList<>(players.size());
        for (int i = 0; i < players.size(); i++) {
            Participant p = players.get(i);
            LlmBrain brain = llmBrainFactory.create(configs.get(i), p.name(), p.archetype(), atelierId);
            agents.add(new LlmPlayerAgent(p.name(), p.archetype(), brain));
        }
        return agents;
    }
}
