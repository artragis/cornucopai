package com.cornucopai.game.agent;

import com.cornucopai.game.PlayerAgent;
import com.cornucopai.game.model.Archetype;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates the player agents for a game (research R-004): players are named
 * {@code Joueur-1..N} and archetypes are assigned round-robin in the fixed
 * order [GRUMPY, HAPPY, GREEDY, PROF].
 */
@ApplicationScoped
public class AgentFactory {

    private static final List<Archetype> ARCHETYPE_ORDER =
            List.of(Archetype.GRUMPY, Archetype.HAPPY, Archetype.GREEDY, Archetype.PROF);

    public List<PlayerAgent> createAgents(int playerCount) {
        List<PlayerAgent> agents = new ArrayList<>(playerCount);
        for (int i = 1; i <= playerCount; i++) {
            String name = "Joueur-" + i;
            agents.add(create(ARCHETYPE_ORDER.get((i - 1) % ARCHETYPE_ORDER.size()), name));
        }
        return agents;
    }

    public PlayerAgent create(Archetype archetype, String name) {
        return switch (archetype) {
            case GRUMPY -> new GrumpyAgent(name);
            case HAPPY -> new HappyAgent(name);
            case GREEDY -> new GreedyAgent(name);
            case PROF -> new ProfAgent(name);
        };
    }
}
