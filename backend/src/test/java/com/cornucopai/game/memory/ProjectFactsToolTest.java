package com.cornucopai.game.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T023 (spec 007 US2): the two shared-memory tools — get_project_facts returns all
 * facts as JSON, add_project_fact appends with author/turnIndex/timestamp filled
 * from the context (R-049, FR-005/FR-006).
 */
class ProjectFactsToolTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void getProjectFactsReturnsEmptyArrayWhenNoFacts() {
        SharedProjectMemory memory = new SharedProjectMemory("atelierX");
        ProjectFactsTool tool = new ProjectFactsTool(memory, () -> "Joueur-1", () -> 3);
        assertThat(tool.getProjectFacts()).isEqualTo("[]");
    }

    @Test
    void addProjectFactAppendsWithContextMetadata() throws Exception {
        SharedProjectMemory memory = new SharedProjectMemory("atelierX");
        ProjectFactsTool tool = new ProjectFactsTool(memory, () -> "Joueur-2", () -> 5);
        String confirmation = tool.addProjectFact("le projet n'expose pas d'API REST");
        assertThat(confirmation).contains("enregistré");
        assertThat(memory.facts()).hasSize(1);
        ProjectFact fact = memory.facts().get(0);
        assertThat(fact.text()).isEqualTo("le projet n'expose pas d'API REST");
        assertThat(fact.author()).isEqualTo("Joueur-2");
        assertThat(fact.turnIndex()).isEqualTo(5);
        assertThat(fact.timestamp()).isNotNull();
    }

    @Test
    void getProjectFactsReturnsAllFactsAsJson() throws Exception {
        SharedProjectMemory memory = new SharedProjectMemory("atelierX");
        memory.add(new ProjectFact("fait A", "Joueur-1", 1, Instant.parse("2026-11-11T10:00:00Z")));
        memory.add(new ProjectFact("fait B", "Joueur-2", 2, Instant.parse("2026-11-11T10:01:00Z")));
        ProjectFactsTool tool = new ProjectFactsTool(memory, () -> "Joueur-1", () -> 1);
        JsonNode json = mapper.readTree(tool.getProjectFacts());
        assertThat(json).hasSize(2);
        assertThat(json.get(0).get("text").asText()).isEqualTo("fait A");
        assertThat(json.get(1).get("author").asText()).isEqualTo("Joueur-2");
    }

    @Test
    void addProjectFactRejectsBlank() {
        SharedProjectMemory memory = new SharedProjectMemory("atelierX");
        ProjectFactsTool tool = new ProjectFactsTool(memory, () -> "Joueur-1", () -> 1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> tool.addProjectFact("  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(memory.facts()).isEmpty();
    }
}
