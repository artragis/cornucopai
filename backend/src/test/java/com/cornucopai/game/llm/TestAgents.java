package com.cornucopai.game.llm;

/** Test helper (spec 009): builds {@link AgentSnapshot}s without touching the filesystem. */
public final class TestAgents {

  private TestAgents() {}

  public static AgentSnapshot snapshot(String id) {
    return new AgentSnapshot(
        id, "Agent-" + id, "http://localhost:1235/v1", "", "test-model", "Personnalité de test.");
  }

  public static AgentSnapshot snapshotWithKeyVar(String id, String apiKeyVar) {
    return new AgentSnapshot(
        id,
        "Agent-" + id,
        "https://api.mistral.ai/v1",
        apiKeyVar,
        "mistral-large-latest",
        "Personnalité de test.");
  }
}
