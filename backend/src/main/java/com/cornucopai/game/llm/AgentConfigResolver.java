package com.cornucopai.game.llm;

import com.cornucopai.api.dto.AgentConfigDto;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Participant;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the per-player participants and LLM configs of a game (spec 004, R-028, R-032): request
 * {@code agents} entries win per player; everything else falls back to {@code Joueur-N} names,
 * round-robin archetypes [GRUMPY, HAPPY, GREEDY, PROF] (R-004) and the server {@link LlmDefaults}
 * (FR-002).
 */
public class AgentConfigResolver {

  private static final List<Archetype> ARCHETYPE_ORDER =
      List.of(Archetype.GRUMPY, Archetype.HAPPY, Archetype.GREEDY, Archetype.PROF);

  private final LlmDefaults defaults;

  public AgentConfigResolver(LlmDefaults defaults) {
    this.defaults = defaults;
  }

  public record Resolved(List<Participant> players, List<AgentLlmConfig> configs) {}

  public Resolved resolve(Integer playerCount, List<AgentConfigDto> agents) {
    int n = playerCount == null ? 0 : playerCount;
    if (agents != null && agents.size() > n) {
      throw new IllegalArgumentException(
          "agents.size() (" + agents.size() + ") exceeds playerCount (" + n + ")");
    }
    List<Participant> players = new ArrayList<>(n);
    List<AgentLlmConfig> configs = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      AgentConfigDto dto = agents != null && i < agents.size() ? agents.get(i) : null;
      String name =
          dto != null && dto.name() != null && !dto.name().isBlank()
              ? dto.name()
              : "Joueur-" + (i + 1);
      Archetype archetype =
          dto != null && dto.archetype() != null
              ? dto.archetype()
              : ARCHETYPE_ORDER.get(i % ARCHETYPE_ORDER.size());
      players.add(new Participant(name, archetype));
      configs.add(dto != null ? dto.toConfig(defaults) : defaults.defaultConfig());
    }
    return new Resolved(List.copyOf(players), List.copyOf(configs));
  }
}
