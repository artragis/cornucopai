package com.cornucopai.game.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.cornucopai.game.model.Archetype;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * T038 (US4): one system prompt per archetype — game rules + output contract + personality, all
 * distinct (FR-011, R-026).
 */
class SystemPromptsTest {

  @Test
  void fourDistinctPromptsOnePerArchetype() {
    Set<String> prompts = new HashSet<>();
    for (Archetype a : EnumSet.allOf(Archetype.class)) {
      prompts.add(SystemPrompts.forArchetype(a));
    }
    assertThat(prompts).hasSize(4);
  }

  @Test
  void everyPromptCarriesGameRulesAndOutputContract() {
    for (Archetype a : EnumSet.allOf(Archetype.class)) {
      String p = SystemPrompts.forArchetype(a);
      assertThat(p).contains("Cornucopia");
      // game rules: play a card, react, answer, vote
      assertThat(p).containsIgnoringCase("carte");
      assertThat(p).containsIgnoringCase("vote");
      // output contract: the exact DTO JSON shapes
      assertThat(p).contains("scenario").contains("explanation");
      assertThat(p).contains("PRECISION").contains("QUESTION").contains("PASS");
      assertThat(p).contains("knows");
      assertThat(p).contains("YES").contains("NO");
      assertThat(p).containsIgnoringCase("JSON");
    }
  }

  @Test
  void eachArchetypeHasItsOwnPersonality() {
    assertThat(SystemPrompts.forArchetype(Archetype.GRUMPY)).containsIgnoringCase("grincheux");
    assertThat(SystemPrompts.forArchetype(Archetype.HAPPY)).containsIgnoringCase("enthousiaste");
    assertThat(SystemPrompts.forArchetype(Archetype.GREEDY)).containsIgnoringCase("longues");
    assertThat(SystemPrompts.forArchetype(Archetype.PROF)).containsIgnoringCase("pédagogue");
  }

  // T035 (spec 009, FR-012): the game-flow prompt is base rules + the free-text personality
  @Test
  void forPersonalityIsBaseRulesPlusFreeTextPrompt() {
    String p = SystemPrompts.forPersonality("Tu es un analyste paranoïaque.");
    assertThat(p)
        .contains("Cornucopia")
        .contains("PRECISION")
        .contains("YES")
        .containsIgnoringCase("JSON");
    assertThat(p).contains("Tu es un analyste paranoïaque.");
    assertThat(p).doesNotContain("GRUMPY").doesNotContain("grincheux");
    // distinct personalities -> distinct prompts
    assertThat(SystemPrompts.forPersonality("A")).isNotEqualTo(SystemPrompts.forPersonality("B"));
  }

  // T010 (spec 010, FR-008): free-text fields are markdown; structured data goes in a ```json
  // code fence — never XML or any other markup.
  @Test
  void freeTextFieldsAreMarkdownAndStructuredDataIsJsonFencedNeverXml() {
    String p = SystemPrompts.forPersonality("Tu es un analyste paranoïaque.");
    assertThat(p).containsIgnoringCase("markdown");
    assertThat(p).contains("```json");
    // the deprecated archetype prompts share RULES_AND_CONTRACT, so they carry the rule too
    for (Archetype a : EnumSet.allOf(Archetype.class)) {
      assertThat(SystemPrompts.forArchetype(a))
          .contains("```json")
          .containsIgnoringCase("markdown");
    }
  }
}
