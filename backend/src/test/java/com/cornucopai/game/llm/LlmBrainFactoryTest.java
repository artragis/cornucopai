package com.cornucopai.game.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * T034 (spec 009, US3): the factory builds the brain from an {@link AgentSnapshot} — fake mode →
 * {@link FakeLlmBrain}; auto → the single OpenAI-compatible {@link LangChain4jBrain} with the
 * agent's URL/model and the API key resolved via the env seam (FR-006/FR-013). No ACP branch exists
 * anymore (FR-011); the system prompt carries the personalityPrompt (FR-012).
 */
class LlmBrainFactoryTest {

  private final LlmDefaults defaults =
      new LlmDefaults("http://localhost:1235/v1", null, null, Duration.ofSeconds(5));

  @Test
  void fakeModeReturnsTheScriptedBrain() {
    LlmBrainFactory factory = new LlmBrainFactory("fake", defaults, List.of());
    LlmBrain brain = factory.create(TestAgents.snapshot("a1"), "Joueur-1", "atelierX");
    assertThat(brain).isInstanceOf(FakeLlmBrain.class);
  }

  @Test
  void autoModeBuildsLangChain4jBrainFromTheSnapshot() {
    LlmBrainFactory factory = new LlmBrainFactory("auto", defaults, List.of());
    LlmBrain brain = factory.create(TestAgents.snapshot("a1"), "Joueur-1", "atelierX");
    assertThat(brain).isInstanceOf(LangChain4jBrain.class);
    LangChain4jBrain lcb = (LangChain4jBrain) brain;
    assertThat(lcb.snapshot().openAiCompatibleUrl()).isEqualTo("http://localhost:1235/v1");
    // no key when apiKeyVar is empty (FR-006)
    assertThat(lcb.apiKey()).isNull();
  }

  @Test
  void apiKeyIsResolvedFromTheEnvVarNamedByApiKeyVar() {
    LlmBrainFactory factory =
        new LlmBrainFactory(
            "auto", defaults, List.of(), var -> Map.of("MY_TEST_KEY", "s3cr3t").get(var));
    LangChain4jBrain brain =
        (LangChain4jBrain)
            factory.create(
                TestAgents.snapshotWithKeyVar("a1", "MY_TEST_KEY"), "Joueur-1", "atelierX");
    assertThat(brain.apiKey()).isEqualTo("s3cr3t");
    assertThat(brain.snapshot().openAiCompatibleUrl()).isEqualTo("https://api.mistral.ai/v1");
  }

  @Test
  void missingEnvVarIsRejectedNamingTheVariable() {
    LlmBrainFactory factory =
        new LlmBrainFactory("auto", defaults, List.of(), var -> null); // env var absent
    assertThatThrownBy(() ->
            factory.create(
                TestAgents.snapshotWithKeyVar("a1", "DEFINITELY_MISSING_VAR"),
                "Joueur-1",
                "atelierX"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("DEFINITELY_MISSING_VAR");
  }

  @Test
  void systemPromptIsBasePlusPersonalityPrompt() {
    LlmBrainFactory factory = new LlmBrainFactory("auto", defaults, List.of());
    LangChain4jBrain brain =
        (LangChain4jBrain)
            factory.create(
                new AgentSnapshot(
                    "a1",
                    "Analyste",
                    "http://localhost:1235/v1",
                    "",
                    "m",
                    "Tu es tatillon et sceptique."),
                "Joueur-1",
                "atelierX");
    assertThat(brain.systemPrompt()).contains("Tu es tatillon et sceptique.");
    assertThat(brain.systemPrompt()).containsIgnoringCase("contrat de sortie").contains("JSON");
    // no archetype prose in the game flow anymore (FR-011/FR-012)
    assertThat(brain.systemPrompt()).doesNotContain("GRUMPY").doesNotContain("archétype");
  }

  @Test
  void noAcpBranchExistsAnymore() {
    // FR-011: ACP is out of the launch path — there is no providerType on the snapshot and no
    // AcpCliBrain class on the classpath
    assertThat(TestAgents.snapshot("a1").getClass().getRecordComponents())
        .extracting(java.lang.reflect.RecordComponent::getName)
        .doesNotContain("providerType", "acpProvider", "command");
    assertThatThrownBy(() -> Class.forName("com.cornucopai.game.llm.AcpCliBrain"))
        .isInstanceOf(ClassNotFoundException.class);
  }
}
