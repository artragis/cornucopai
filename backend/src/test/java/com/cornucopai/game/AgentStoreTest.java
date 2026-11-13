package com.cornucopai.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.cornucopai.game.model.Agent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * T003 (spec 009, Phase 2): AgentStore CRUD — one {@code {id}.json} file per agent, atomic write,
 * sorted listing, delete (research R-001).
 */
class AgentStoreTest {

  @TempDir Path tempDir;

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  private AgentStore store;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    store = new AgentStore(tempDir, mapper);
  }

  private static Agent agent(String name) {
    return Agent.create(name, "http://localhost:1235/v1", "", "model-x", "personnalité de " + name);
  }

  @Test
  void saveWritesExactlyOneJsonFileNamedById() {
    Agent a = store.save(agent("Alpha"));
    assertThat(tempDir.resolve(a.id() + ".json")).exists();
    assertThat(tempDir)
        .isDirectoryContaining(p -> p.getFileName().toString().equals(a.id() + ".json"));
    // no leftover tmp file (atomic write)
    assertThat(tempDir.resolve(a.id() + ".json.tmp")).doesNotExist();
  }

  @Test
  void loadRoundTripsAndUnknownIdIsEmpty() {
    Agent a = store.save(agent("Alpha"));
    assertThat(store.load(a.id())).contains(a);
    assertThat(store.load("00000000-0000-0000-0000-000000000000")).isEmpty();
    assertThat(store.load(null)).isEmpty();
  }

  @Test
  void listAllReturnsAgentsSortedByName() {
    store.save(agent("Zeta"));
    store.save(agent("alpha"));
    store.save(agent("Beta"));
    assertThat(store.listAll())
        .extracting(Agent::agentName)
        .containsExactly("alpha", "Beta", "Zeta");
  }

  @Test
  void saveOverwritesTheSameFileKeepingTheId() {
    Agent a = store.save(agent("Alpha"));
    Agent updated = new Agent(a.id(), "Alpha2", "https://api.mistral.ai/v1", "K", "m2", "p2");
    store.save(updated);
    assertThat(store.load(a.id())).contains(updated);
    assertThat(store.listAll()).hasSize(1);
  }

  @Test
  void deleteRemovesTheFileAndReportsUnknownIds() {
    Agent a = store.save(agent("Alpha"));
    assertThat(store.delete(a.id())).isTrue();
    assertThat(store.load(a.id())).isEmpty();
    assertThat(store.delete(a.id())).isFalse();
    assertThat(Files.exists(tempDir.resolve(a.id() + ".json"))).isFalse();
  }

  @Test
  void nameExistsIgnoresTheExcludedId() {
    Agent a = store.save(agent("Alpha"));
    Agent b = store.save(agent("Beta"));
    assertThat(store.nameExists("Alpha", b.id())).isTrue();
    assertThat(store.nameExists("Alpha", a.id())).isFalse();
    assertThat(store.nameExists("Gamma", a.id())).isFalse();
    // comparison is on the trimmed value
    assertThat(store.nameExists("  Alpha  ", b.id())).isTrue();
  }

  @Test
  void unreadableAgentFileIsSkippedNotFatal() throws Exception {
    store.save(agent("Alpha"));
    Files.writeString(tempDir.resolve("broken.json"), "{not json");
    List<Agent> all = store.listAll();
    assertThat(all).extracting(Agent::agentName).containsExactly("Alpha");
  }
}
