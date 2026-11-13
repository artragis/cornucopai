package com.cornucopai.game.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cornucopai.game.model.Agent;
import com.cornucopai.game.model.Participant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * T019 (spec 009, US2): resolve(List&lt;String&gt; agentIds, Random) — unknown id → error, &lt; 2
 * entries → error (FR-010), duplicate id allowed → two participants sharing one agentId (FR-009),
 * participant order drawn randomly with the association preserved and names Joueur-N assigned AFTER
 * the shuffle (FR-016/FR-020), snapshots index-aligned (FR-017).
 */
class AgentConfigResolverTest {

  private final Map<String, Agent> agents = new HashMap<>();
  private final AgentConfigResolver resolver =
      new AgentConfigResolver(id -> Optional.ofNullable(agents.get(id)));

  /** Adds an agent with a random UUID id and returns that id. */
  private String addAgent(String name) {
    String id = UUID.randomUUID().toString();
    agents.put(
        id, new Agent(id, name, "http://localhost:1235/v1", "", "model-x", "personnalité " + name));
    return id;
  }

  @Test
  void twoDistinctAgentsProduceTwoParticipantsWithSnapshots() {
    String a1 = addAgent("Alpha");
    String a2 = addAgent("Beta");
    AgentConfigResolver.Resolved r = resolver.resolve(List.of(a1, a2), new Random(42));
    assertThat(r.players()).hasSize(2);
    assertThat(r.snapshots()).hasSize(2);
    for (int i = 0; i < 2; i++) {
      Participant p = r.players().get(i);
      assertThat(p.name()).isEqualTo("Joueur-" + (i + 1));
      AgentSnapshot s = r.snapshots().get(i);
      assertThat(s.id()).isEqualTo(p.agentId());
      assertThat(s.personalityPrompt()).startsWith("personnalité ");
    }
    assertThat(r.players()).extracting(Participant::agentId).containsExactlyInAnyOrder(a1, a2);
  }

  @Test
  void sameAgentTwiceIsAllowedAndBothParticipantsShareTheAgentId() {
    String a1 = addAgent("Alpha");
    AgentConfigResolver.Resolved r = resolver.resolve(List.of(a1, a1), new Random(42));
    assertThat(r.players()).hasSize(2);
    assertThat(r.players()).allSatisfy(p -> assertThat(p.agentId()).isEqualTo(a1));
    assertThat(r.players()).extracting(Participant::name).containsExactly("Joueur-1", "Joueur-2");
    assertThat(r.snapshots()).hasSize(2);
    assertThat(r.snapshots()).allSatisfy(s -> assertThat(s.id()).isEqualTo(a1));
  }

  @Test
  void unknownAgentIdIsRejected() {
    String a1 = addAgent("Alpha");
    String unknown = UUID.randomUUID().toString();
    assertThatThrownBy(() -> resolver.resolve(List.of(a1, unknown), new Random(42)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unknown agent id: " + unknown);
  }

  @Test
  void lessThanTwoAgentIdsAreRejected() {
    String a1 = addAgent("Alpha");
    assertThatThrownBy(() -> resolver.resolve(List.of(a1), new Random(42)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least 2");
    assertThatThrownBy(() -> resolver.resolve(null, new Random(42)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> resolver.resolve(List.of(), new Random(42)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void seededShufflePreservesAssociationButChangesOrder() {
    String a1 = addAgent("Alpha");
    String a2 = addAgent("Beta");
    String a3 = addAgent("Gamma");
    for (long seed = 0; seed < 100; seed++) {
      AgentConfigResolver.Resolved r = resolver.resolve(List.of(a1, a2, a3), new Random(seed));
      List<String> ids = r.players().stream().map(Participant::agentId).toList();
      if (!ids.equals(List.of(a1, a2, a3))) {
        // association preserved: each participant's snapshot id matches its agentId
        for (int i = 0; i < 3; i++) {
          assertThat(r.snapshots().get(i).id()).isEqualTo(r.players().get(i).agentId());
        }
        // names are assigned AFTER the shuffle: Joueur-1 is the first of the drawn order
        assertThat(r.players().get(0).name()).isEqualTo("Joueur-1");
        assertThat(ids).containsExactlyInAnyOrder(a1, a2, a3);
        return;
      }
    }
    throw new AssertionError("no permuting seed found");
  }

  @Test
  void snapshotCarriesFullAgentConfigButNeverKeyValue() {
    String keyId = UUID.randomUUID().toString();
    agents.put(
        keyId,
        new Agent(
            keyId,
            "Alpha",
            "https://api.mistral.ai/v1",
            "MISTRAL_API_KEY",
            "mistral-large",
            "Tu es tatillon."));
    String a2 = addAgent("Beta");
    AgentConfigResolver.Resolved r = resolver.resolve(List.of(keyId, a2), new Random(7));
    AgentSnapshot s =
        r.snapshots().stream().filter(x -> x.id().equals(keyId)).findFirst().orElseThrow();
    assertThat(s.openAiCompatibleUrl()).isEqualTo("https://api.mistral.ai/v1");
    assertThat(s.apiKeyVar()).isEqualTo("MISTRAL_API_KEY");
    assertThat(s.modelName()).isEqualTo("mistral-large");
    assertThat(s.personalityPrompt()).isEqualTo("Tu es tatillon.");
    // FR-017: no apiKey field exists on the snapshot at all
    assertThat(s.getClass().getRecordComponents())
        .extracting(java.lang.reflect.RecordComponent::getName)
        .doesNotContain("apiKey");
  }
}
