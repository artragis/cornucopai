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
}
