package com.cornucopai.game.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProjectFactModelTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void recordExposesAllFields() {
    Instant now = Instant.parse("2026-11-11T14:32:10Z");
    ProjectFact fact = new ProjectFact("pas d'API REST publique", "Joueur-2", 3, now);
    assertThat(fact.text()).isEqualTo("pas d'API REST publique");
    assertThat(fact.author()).isEqualTo("Joueur-2");
    assertThat(fact.turnIndex()).isEqualTo(3);
    assertThat(fact.timestamp()).isEqualTo(now);
  }

  @Test
  void textMustBeNonBlank() {
    Instant now = Instant.now();
    assertThatThrownBy(() -> new ProjectFact("", "a", 1, now))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ProjectFact(null, "a", 1, now))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void roundTripsThroughJackson() throws Exception {
    ProjectFact fact =
        new ProjectFact(
            "auth en session cookie", "Joueur-1", 2, Instant.parse("2026-11-11T10:00:00Z"));
    String json = mapper.writeValueAsString(fact);
    ProjectFact read = mapper.readValue(json, ProjectFact.class);
    assertThat(read).isEqualTo(fact);
  }
}
