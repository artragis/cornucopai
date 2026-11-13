package com.cornucopai.game.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.cornucopai.game.PlayerAgent;
import com.cornucopai.game.llm.AgentSnapshot;
import com.cornucopai.game.llm.FakeLlmBrain;
import com.cornucopai.game.llm.LlmBrainFactory;
import com.cornucopai.game.llm.LlmDefaults;
import com.cornucopai.game.llm.TestAgents;
import com.cornucopai.game.model.Participant;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * T022 (US2): games are played by LlmPlayerAgent only — deterministic agents are out of the game
 * path (FR-003).
 */
class AgentFactoryTest {

  private final LlmBrainFactory brainFactory =
      new LlmBrainFactory(
          "fake",
          new LlmDefaults("http://localhost:1235/v1", null, null, Duration.ofSeconds(5)),
          List.of());
  private final AgentFactory factory = new AgentFactory(brainFactory);

  @Test
  void createAgentsBuildsLlmPlayerAgentsWithMatchingNamesAndAgentIds() {
    List<Participant> players =
        List.of(new Participant("Joueur-1", "agent-1"), new Participant("Joueur-2", "agent-2"));
    List<AgentSnapshot> snapshots =
        List.of(TestAgents.snapshot("agent-1"), TestAgents.snapshot("agent-2"));
    List<PlayerAgent> agents = factory.createAgents(players, snapshots, "atelierTest");
    assertThat(agents).hasSize(2);
    assertThat(agents).allSatisfy(a -> assertThat(a).isInstanceOf(LlmPlayerAgent.class));
    assertThat(agents).extracting(PlayerAgent::name).containsExactly("Joueur-1", "Joueur-2");
    assertThat(agents)
        .extracting(a -> players.get(agents.indexOf(a)).agentId())
        .containsExactly("agent-1", "agent-2");
    // each agent got its own brain instance
    assertThat(((LlmPlayerAgent) agents.get(0)).brain())
        .isNotSameAs(((LlmPlayerAgent) agents.get(1)).brain());
    assertThat(((LlmPlayerAgent) agents.get(0)).brain()).isInstanceOf(FakeLlmBrain.class);
    // no deterministic agent in the game path
    assertThat(agents).noneSatisfy(a -> assertThat(a).isInstanceOf(GreedyAgent.class));
  }
}
