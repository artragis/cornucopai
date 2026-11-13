package com.cornucopai.game;

import com.cornucopai.game.model.Agent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/**
 * Business operations on configurable agents (spec 009): CRUD + validation + {@code agentName}
 * uniqueness (FR-018) + id immutability (FR-003). Validation rules live in {@link Agent} (FR-019).
 */
@ApplicationScoped
public class AgentService {

  private final AgentStore store;

  @Inject
  public AgentService(AgentStore store) {
    this.store = store;
  }

  /** Test-friendly constructor. */
  public AgentService(AgentStore store, boolean ignored) {
    this.store = store;
  }

  public List<Agent> list() {
    return store.listAll();
  }

  public Agent get(String id) {
    return store.load(id).orElseThrow(() -> new AgentNotFoundException(id));
  }

  /** Creates an agent with a server-generated UUID id (FR-003). */
  public Agent create(
      String agentName,
      String openAiCompatibleUrl,
      String apiKeyVar,
      String modelName,
      String personalityPrompt) {
    Agent candidate =
        Agent.create(agentName, openAiCompatibleUrl, apiKeyVar, modelName, personalityPrompt);
    if (store.nameExists(candidate.agentName(), candidate.id())) {
      throw new AgentNameConflictException(candidate.agentName());
    }
    return store.save(candidate);
  }

  /** Updates an agent; the id in the path is authoritative and never changes (FR-003/FR-004). */
  public Agent update(
      String id,
      String agentName,
      String openAiCompatibleUrl,
      String apiKeyVar,
      String modelName,
      String personalityPrompt) {
    Agent existing = get(id); // 404 when unknown
    Agent updated =
        new Agent(id, agentName, openAiCompatibleUrl, apiKeyVar, modelName, personalityPrompt);
    if (store.nameExists(updated.agentName(), updated.id())) {
      throw new AgentNameConflictException(updated.agentName());
    }
    return store.save(updated);
  }

  /** Deletes an agent (FR-005); existing ateliers keep their launch snapshot (FR-017). */
  public void delete(String id) {
    get(id); // 404 when unknown
    store.delete(id);
  }
}
