package com.cornucopai.game.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A player in an atelier (spec 009): a name plus the id of the configurable {@link Agent} that
 * drives it. The old {@code archetype} enum is no longer used by the game flow (FR-011): it stays
 * as a deprecated, null-by-default field so legacy atelier.json files keep deserializing (FR-014)
 * and the deterministic agents (unit tests only) keep working. New ateliers never write it ({@code
 * NON_NULL}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Participant(String name, String agentId, Archetype archetype) {

  /** New code: a participant is a name + the id of its driving agent. */
  public Participant(String name, String agentId) {
    this(name, agentId, null);
  }

  /**
   * Legacy/test-only constructor for the deterministic archetype agents.
   *
   * @deprecated spec 009 (FR-011): use {@link #Participant(String, String)}.
   */
  @Deprecated
  public Participant(String name, Archetype archetype) {
    this(name, null, archetype);
  }
}
