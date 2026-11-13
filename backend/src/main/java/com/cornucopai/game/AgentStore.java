package com.cornucopai.game;

import com.cornucopai.game.model.Agent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Filesystem persistence of configurable agents (spec 009, research R-001): one JSON file {@code
 * {rootDir}/{id}.json} per agent, written atomically (tmp file + move). Mirrors {@link
 * AtelierStore}.
 */
@ApplicationScoped
public class AgentStore {

  private static final Logger LOG = Logger.getLogger(AgentStore.class);

  private final Path rootDir;
  private final ObjectMapper mapper;

  @Inject
  public AgentStore(
      @ConfigProperty(name = "cornucopai.agents.root-dir", defaultValue = "agents") String rootDir,
      ObjectMapper mapper) {
    this(Path.of(rootDir), mapper);
  }

  /** Test-friendly constructor. */
  public AgentStore(Path rootDir, ObjectMapper mapper) {
    this.rootDir = rootDir;
    this.mapper = mapper;
  }

  /** Creates or overwrites the agent file {@code {id}.json} (atomic write). */
  public Agent save(Agent agent) {
    try {
      Files.createDirectories(rootDir);
      Path file = rootDir.resolve(agent.id() + ".json");
      Path tmp = rootDir.resolve(agent.id() + ".json.tmp");
      mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), agent);
      Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      return agent;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Optional<Agent> load(String id) {
    if (id == null || id.isBlank()) {
      return Optional.empty();
    }
    Path file = rootDir.resolve(id + ".json");
    if (!Files.exists(file)) {
      return Optional.empty();
    }
    try {
      return Optional.of(mapper.readValue(file.toFile(), Agent.class));
    } catch (IOException e) {
      LOG.warnf("Skipping unreadable agent file %s: %s", file, e.getMessage());
      return Optional.empty();
    }
  }

  /** All agents, sorted by agentName (contract: GET /api/agents). */
  public List<Agent> listAll() {
    if (!Files.isDirectory(rootDir)) {
      return List.of();
    }
    try (Stream<Path> files = Files.list(rootDir)) {
      return files
          .filter(p -> p.getFileName().toString().endsWith(".json"))
          .map(p -> readQuietly(p))
          .flatMap(Optional::stream)
          .sorted(Comparator.comparing(Agent::agentName, String.CASE_INSENSITIVE_ORDER))
          .toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** True when another agent (different id) already uses this agentName (FR-018). */
  public boolean nameExists(String agentName, String excludeId) {
    String wanted = agentName == null ? "" : agentName.trim();
    return listAll().stream()
        .anyMatch(a -> !a.id().equals(excludeId) && a.agentName().trim().equals(wanted));
  }

  /** Deletes the agent file (FR-005); returns false when the id was unknown. */
  public boolean delete(String id) {
    if (id == null || id.isBlank()) {
      return false;
    }
    try {
      return Files.deleteIfExists(rootDir.resolve(id + ".json"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Optional<Agent> readQuietly(Path file) {
    try {
      return Optional.of(mapper.readValue(file.toFile(), Agent.class));
    } catch (IOException e) {
      LOG.warnf("Skipping unreadable agent file %s: %s", file, e.getMessage());
      return Optional.empty();
    }
  }
}
