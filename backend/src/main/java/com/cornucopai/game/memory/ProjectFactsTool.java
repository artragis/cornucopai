package com.cornucopai.game.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import java.time.Instant;
import java.util.function.Supplier;
import org.jboss.logging.Logger;

/**
 * The two shared-memory tools exposed to every agent of an atelier (spec 007 US2, R-049,
 * FR-005/FR-006): {@code get_project_facts} returns all recorded project facts as JSON, {@code
 * add_project_fact} appends one. The author and turn index are taken from suppliers wired to the
 * calling agent / current leg, so the model only provides the fact text.
 */
public class ProjectFactsTool {

  private static final Logger LOG = Logger.getLogger(ProjectFactsTool.class);
  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

  private final SharedProjectMemory memory;
  private final Supplier<String> currentAgent;
  private final Supplier<Integer> currentTurnIndex;

  public ProjectFactsTool(
      SharedProjectMemory memory,
      Supplier<String> currentAgent,
      Supplier<Integer> currentTurnIndex) {
    this.memory = memory;
    this.currentAgent = currentAgent;
    this.currentTurnIndex = currentTurnIndex;
  }

  @Tool(
      name = "get_project_facts",
      value =
          """
          Retourne tous les faits découverts sur le projet analysé par les agents de cette partie \
          (texte, auteur, numéro du tour, horodatage). Utilise cet outil pour savoir ce qui a déjà été \
          découvert sur le projet avant de re-vérifier via les outils MCP.\
          """)
  public String getProjectFacts() {
    LOG.infof(
        "tool get_project_facts atelier=%s count=%d", memory.atelierId(), memory.facts().size());
    try {
      return MAPPER.writeValueAsString(memory.facts());
    } catch (JsonProcessingException e) {
      LOG.warnf(e, "Cannot serialize project facts");
      return "[]";
    }
  }

  @Tool(
      name = "add_project_fact",
      value =
          """
          Enregistre un fait découvert sur le projet analysé (ex. « le projet n'expose pas d'API REST \
          publique », « l'authentification est en session cookie »). Le fait sera visible par tous les \
          agents de la partie via get_project_facts. N'enregistre que des faits vérifiés ou fortement \
          probables.\
          """)
  public String addProjectFact(String fact) {
    if (fact == null || fact.isBlank()) {
      throw new IllegalArgumentException("fact must be non-blank");
    }
    ProjectFact pf =
        new ProjectFact(fact, currentAgent.get(), currentTurnIndex.get(), Instant.now());
    memory.add(pf);
    LOG.infof(
        "tool add_project_fact atelier=%s author=%s fact=%s",
        memory.atelierId(), pf.author(), fact);
    return "Fait enregistré (" + memory.facts().size() + " au total).";
  }
}
