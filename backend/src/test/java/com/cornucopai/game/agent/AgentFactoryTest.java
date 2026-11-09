package com.cornucopai.game.agent;

import com.cornucopai.game.PlayerAgent;
import com.cornucopai.game.llm.AgentLlmConfig;
import com.cornucopai.game.llm.FakeLlmBrain;
import com.cornucopai.game.llm.LlmBrainFactory;
import com.cornucopai.game.llm.LlmDefaults;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Participant;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** T022 (US2): games are played by LlmPlayerAgent only — deterministic agents are out of the game path (FR-003). */
class AgentFactoryTest {

    private final LlmBrainFactory brainFactory =
            new LlmBrainFactory("fake", new LlmDefaults("http://localhost:1235/v1", null, null, Duration.ofSeconds(5)), List.of());
    private final AgentFactory factory = new AgentFactory(brainFactory);

    @Test
    void createAgentsBuildsLlmPlayerAgentsWithMatchingNamesAndArchetypes() {
        List<Participant> players = List.of(
                new Participant("Joueur-1", Archetype.GRUMPY),
                new Participant("Joueur-2", Archetype.HAPPY));
        List<AgentLlmConfig> configs = List.of(
                AgentLlmConfig.defaultOpenAiCompatible("http://localhost:1235/v1"),
                AgentLlmConfig.defaultOpenAiCompatible("http://localhost:1235/v1"));
        List<PlayerAgent> agents = factory.createAgents(players, configs, "atelierTest");
        assertThat(agents).hasSize(2);
        assertThat(agents).allSatisfy(a -> assertThat(a).isInstanceOf(LlmPlayerAgent.class));
        assertThat(agents).extracting(PlayerAgent::name).containsExactly("Joueur-1", "Joueur-2");
        assertThat(agents).extracting(PlayerAgent::archetype)
                .containsExactly(Archetype.GRUMPY, Archetype.HAPPY);
        // each agent got its own brain instance
        assertThat(((LlmPlayerAgent) agents.get(0)).brain())
                .isNotSameAs(((LlmPlayerAgent) agents.get(1)).brain());
        assertThat(((LlmPlayerAgent) agents.get(0)).brain()).isInstanceOf(FakeLlmBrain.class);
        // no deterministic agent in the game path
        assertThat(agents).noneSatisfy(a -> assertThat(a).isInstanceOf(GreedyAgent.class));
    }
}
