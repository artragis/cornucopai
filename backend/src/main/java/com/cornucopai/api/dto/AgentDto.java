package com.cornucopai.api.dto;

import com.cornucopai.game.model.Agent;

/**
 * Agent DTO of {@code contracts/agents-api.yaml} v1.0.0 (spec 009). {@link AgentInput} is the
 * create/update body (no id — the server generates it, FR-003); {@link AgentDto} is the serialized
 * agent (with id). Only the {@code apiKeyVar} NAME crosses the API — never a key value (FR-006).
 */
public record AgentDto(
    String id,
    String agentName,
    String openAiCompatibleUrl,
    String apiKeyVar,
    String modelName,
    String personalityPrompt) {

  public static AgentDto of(Agent agent) {
    return new AgentDto(
        agent.id(),
        agent.agentName(),
        agent.openAiCompatibleUrl(),
        agent.apiKeyVar(),
        agent.modelName(),
        agent.personalityPrompt());
  }

  /** POST/PUT body (contracts/agents-api.yaml v1.0.0, schema AgentInput). */
  public record AgentInput(
      String agentName,
      String openAiCompatibleUrl,
      String apiKeyVar,
      String modelName,
      String personalityPrompt) {}
}
