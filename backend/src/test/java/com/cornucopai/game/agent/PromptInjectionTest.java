package com.cornucopai.game.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.cornucopai.game.llm.FakeLlmBrain;
import com.cornucopai.game.llm.LegContext;
import com.cornucopai.game.memory.ProjectFact;
import com.cornucopai.game.memory.SharedProjectMemory;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * T026 (spec 007 US2): with more than 20 facts in the shared memory, the vote prompt contains
 * exactly the 20 most recent, inside a DONNÉES section — never in the system prompt (FR-008,
 * clarification 2026-11-11).
 */
class PromptInjectionTest {

  @Test
  void votePromptContainsExactlyThe20MostRecentFactsAsDonnees() {
    SharedProjectMemory memory = new SharedProjectMemory("atelierX");
    for (int i = 1; i <= 25; i++) {
      memory.add(
          new ProjectFact("fait " + i, "Joueur-1", i, Instant.parse("2026-11-11T10:00:00Z")));
    }
    LlmPlayerAgent agent =
        new LlmPlayerAgent("Joueur-1", new FakeLlmBrain(), new LegContext("atelierX"), memory);
    Leg leg = new Leg("VE2", "scénario", "explication");
    Participant cardPlayer = new Participant("Joueur-2", Archetype.HAPPY);

    String prompt = agent.votePromptWithFacts(leg, cardPlayer);

    assertThat(prompt).contains("FAITS DÉCOUVERTS SUR LE PROJET");
    assertThat(prompt).contains("DONNÉES");
    assertThat(prompt).contains("fait 6"); // oldest of the 20 most recent
    assertThat(prompt).contains("fait 25"); // newest
    assertThat(prompt).doesNotContain("fait 5"); // older than the window
    // count the fact lines: exactly 20
    long factLines = prompt.lines().filter(l -> l.startsWith("- [tour ")).count();
    assertThat(factLines).isEqualTo(20);
  }

  @Test
  void votePromptWithoutMemoryHasNoFactsSection() {
    LlmPlayerAgent agent =
        new LlmPlayerAgent("Joueur-1", new FakeLlmBrain(), new LegContext("atelierX"));
    Leg leg = new Leg("VE2", "scénario", "explication");
    String prompt = agent.votePromptWithFacts(leg, new Participant("Joueur-2", Archetype.HAPPY));
    assertThat(prompt).doesNotContain("FAITS DÉCOUVERTS");
  }
}
