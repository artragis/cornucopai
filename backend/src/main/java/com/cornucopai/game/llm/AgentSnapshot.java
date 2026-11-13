package com.cornucopai.game.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Immutable copy of an agent's configuration at atelier launch (spec 009, FR-017, research R-005):
 * persisted in {@code atelier.json} as {@code agentSnapshots[]}, index-aligned with {@code
 * players[]}. Never contains the API key value — only the {@code apiKeyVar} name, which is resolved
 * via {@code System.getenv} at brain creation (FR-006/FR-013).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentSnapshot(
    String id,
    String agentName,
    String openAiCompatibleUrl,
    String apiKeyVar,
    String modelName,
    String personalityPrompt) {

  public AgentSnapshot {
    if (apiKeyVar == null) {
      apiKeyVar = "";
    }
  }
}
