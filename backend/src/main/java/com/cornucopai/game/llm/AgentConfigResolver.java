package com.cornucopai.game.llm;

import com.cornucopai.game.AgentStore;
import com.cornucopai.game.model.Agent;
import com.cornucopai.game.model.Participant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;

/**
 * Resolves the participants and agent snapshots of a game from the selected agent ids (spec 009,
 * US2, research R-004/R-005): each {@code agentIds} entry binds one participant to that agent
 * (duplicates allowed, FR-009); the participant order — and thus the first leader — is drawn
 * randomly at launch (FR-020); names are {@code Joueur-N} assigned AFTER the shuffle (FR-016); a
 * full {@link AgentSnapshot} of each agent is produced for persistence (FR-017).
 */
public class AgentConfigResolver {

  /** Resolved participants and agent snapshots of a game, index-aligned (FR-017). */
  public record Resolved(List<Participant> players, List<AgentSnapshot> snapshots) {}

  private final Function<String, Optional<Agent>> agentLookup;

  public AgentConfigResolver(AgentStore store) {
    this(store::load);
  }

  /** Test-friendly constructor with an explicit lookup. */
  public AgentConfigResolver(Function<String, Optional<Agent>> agentLookup) {
    this.agentLookup = agentLookup;
  }

  /**
   * Resolves the selected agent ids into participants + snapshots.
   *
   * @param agentIds selected agent ids, at least 2 (FR-010)
   * @param random source of randomness for the participant order (FR-020); seeded in tests
   */
  public Resolved resolve(List<String> agentIds, Random random) {
    if (agentIds == null || agentIds.size() < 2) {
      throw new IllegalArgumentException("agentIds must contain at least 2 entries (FR-010)");
    }
    List<Agent> agents = new ArrayList<>(agentIds.size());
    for (String id : agentIds) {
      agents.add(
          agentLookup
              .apply(id)
              .orElseThrow(() -> new IllegalArgumentException("unknown agent id: " + id)));
    }
    // FR-020: shuffle the participant order; the participant↔agent association is preserved
    List<Integer> order = new ArrayList<>(agents.size());
    for (int i = 0; i < agents.size(); i++) {
      order.add(i);
    }
    java.util.Collections.shuffle(order, random);
    List<Participant> players = new ArrayList<>(agents.size());
    List<AgentSnapshot> snapshots = new ArrayList<>(agents.size());
    for (int pos = 0; pos < order.size(); pos++) {
      Agent agent = agents.get(order.get(pos));
      // FR-016: Joueur-N, N = position in the order drawn at launch
      players.add(new Participant("Joueur-" + (pos + 1), agent.id()));
      snapshots.add(agent.toSnapshot());
    }
    return new Resolved(List.copyOf(players), List.copyOf(snapshots));
  }
}
