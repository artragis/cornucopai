package com.cornucopai.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cornucopai.game.model.Agent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * T004 (spec 009, Phase 2): AgentService — FR-019 validation, FR-018 agentName uniqueness (409),
 * FR-003 id generation + immutability, CRUD (FR-001/002/004/005).
 */
class AgentServiceTest {

  @TempDir Path tempDir;

  private AgentService service;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    service =
        new AgentService(new AgentStore(tempDir, new ObjectMapper().findAndRegisterModules()));
  }

  private Agent createOne(String name) {
    return service.create(name, "http://localhost:1235/v1", "", "model-x", "personnalité");
  }

  @Test
  void createAssignsStableUuidAndPersists() {
    Agent a = createOne("Alpha");
    assertThat(a.id()).matches("[0-9a-f-]{36}");
    assertThat(service.get(a.id())).isEqualTo(a);
    assertThat(service.list()).extracting(Agent::agentName).containsExactly("Alpha");
  }

  @Test
  void createValidatesFieldsPerFr019() {
    assertThatThrownBy(() -> service.create(" ", "http://x", "", "m", "p"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("agentName");
    assertThatThrownBy(() -> service.create("A", "not-an-url", "", "m", "p"))
        .hasMessageContaining("openAiCompatibleUrl");
    assertThatThrownBy(() -> service.create("A", "http://x", "BAD-NAME", "m", "p"))
        .hasMessageContaining("apiKeyVar");
    assertThatThrownBy(() -> service.create("A", "http://x", "", "", "p"))
        .hasMessageContaining("modelName");
    assertThatThrownBy(() -> service.create("A", "http://x", "", "m", null))
        .hasMessageContaining("personalityPrompt");
  }

  @Test
  void duplicateAgentNameIsRejectedAsConflict() {
    createOne("Alpha");
    assertThatThrownBy(() -> createOne("Alpha"))
        .isInstanceOf(AgentNameConflictException.class)
        .hasMessageContaining("Alpha");
    // trimmed comparison
    assertThatThrownBy(() -> createOne("  Alpha ")).isInstanceOf(AgentNameConflictException.class);
  }

  @Test
  void updateKeepsTheIdAndChecksUniquenessAgainstOtherAgents() {
    Agent a = createOne("Alpha");
    Agent b = createOne("Beta");
    Agent updated =
        service.update(a.id(), "Alpha-renamed", "https://api.mistral.ai/v1", "MY_KEY", "m2", "p2");
    assertThat(updated.id()).isEqualTo(a.id());
    assertThat(updated.agentName()).isEqualTo("Alpha-renamed");
    assertThat(updated.apiKeyVar()).isEqualTo("MY_KEY");
    // renaming onto the other agent's name -> conflict
    assertThatThrownBy(() -> service.update(a.id(), "Beta", "http://x", "", "m", "p"))
        .isInstanceOf(AgentNameConflictException.class);
    // keeping its own name is fine
    assertThat(service.update(a.id(), "Alpha-renamed", "http://x", "", "m", "p").agentName())
        .isEqualTo("Alpha-renamed");
    assertThat(service.get(b.id()).agentName()).isEqualTo("Beta");
  }

  @Test
  void updateAndDeleteOfUnknownIdThrowNotFound() {
    String unknown = "00000000-0000-0000-0000-000000000000";
    assertThatThrownBy(() -> service.get(unknown)).isInstanceOf(AgentNotFoundException.class);
    assertThatThrownBy(() -> service.update(unknown, "A", "http://x", "", "m", "p"))
        .isInstanceOf(AgentNotFoundException.class);
    assertThatThrownBy(() -> service.delete(unknown)).isInstanceOf(AgentNotFoundException.class);
  }

  @Test
  void deleteRemovesTheAgentFromTheListing() {
    Agent a = createOne("Alpha");
    service.delete(a.id());
    assertThat(service.list()).isEmpty();
    assertThatThrownBy(() -> service.get(a.id())).isInstanceOf(AgentNotFoundException.class);
  }
}
