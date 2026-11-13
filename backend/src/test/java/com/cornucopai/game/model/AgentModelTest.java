package com.cornucopai.game.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * T002 (spec 009, Phase 2): {@link Agent} Jackson round-trip per contracts/agent-schema.json +
 * FR-019 validation rules (research R-003).
 */
class AgentModelTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  private static Agent valid() {
    return Agent.create(
        "Analyste sécurité",
        "https://api.mistral.ai/v1",
        "MISTRAL_API_KEY",
        "mistral-large-latest",
        "Tu es un analyste sécurité tatillon.");
  }

  @Test
  void jacksonRoundTripPreservesAllFields() throws Exception {
    Agent agent = valid();
    String json = mapper.writeValueAsString(agent);
    Agent back = mapper.readValue(json, Agent.class);
    assertThat(back).isEqualTo(agent);
    assertThat(json).contains("\"id\":\"" + agent.id() + "\"");
    assertThat(json).contains("\"apiKeyVar\":\"MISTRAL_API_KEY\"");
    // the key VALUE must never appear — only the variable name
    assertThat(json).doesNotContain("secret");
  }

  @Test
  void idMustBeUuid() {
    assertThat(Agent.create("A", "http://x", "", "m", "p").id()).matches("[0-9a-f-]{36}");
    assertThatThrownBy(() -> new Agent("not-a-uuid", "A", "http://x", "", "m", "p"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("UUID");
  }

  @Test
  void createGeneratesUuidAndIsImmutable() {
    Agent a = valid();
    assertThat(a.id()).matches("[0-9a-f-]{36}");
  }

  @Test
  void requiredFieldsMustBeNonBlank() {
    assertThatThrownBy(() -> Agent.create(" ", "http://x", "", "m", "p"))
        .hasMessageContaining("agentName");
    assertThatThrownBy(() -> Agent.create("A", "http://x", "", "", "p"))
        .hasMessageContaining("modelName");
    assertThatThrownBy(() -> Agent.create("A", "http://x", "", "m", " "))
        .hasMessageContaining("personalityPrompt");
  }

  @Test
  void openAiCompatibleUrlMustBeAbsoluteHttpOrHttps() {
    assertThat(Agent.isHttpUrl("http://localhost:1235/v1")).isTrue();
    assertThat(Agent.isHttpUrl("https://api.mistral.ai/v1")).isTrue();
    assertThat(Agent.isHttpUrl("ftp://x")).isFalse();
    assertThat(Agent.isHttpUrl("not a url")).isFalse();
    assertThat(Agent.isHttpUrl("localhost:1235")).isFalse();
    assertThat(Agent.isHttpUrl(null)).isFalse();
    assertThatThrownBy(() -> Agent.create("A", "example.com/v1", "", "m", "p"))
        .hasMessageContaining("openAiCompatibleUrl");
  }

  @Test
  void apiKeyVarEmptyMeansNoKeyOtherwiseValidEnvVarName() {
    assertThat(Agent.ENV_VAR_NAME.matcher("").matches()).isTrue();
    assertThat(Agent.ENV_VAR_NAME.matcher("MISTRAL_API_KEY").matches()).isTrue();
    assertThat(Agent.ENV_VAR_NAME.matcher("_X9").matches()).isTrue();
    assertThat(Agent.ENV_VAR_NAME.matcher("9BAD").matches()).isFalse();
    assertThat(Agent.ENV_VAR_NAME.matcher("HAS-DASH").matches()).isFalse();
    assertThatThrownBy(() -> Agent.create("A", "http://x", "9BAD", "m", "p"))
        .hasMessageContaining("apiKeyVar");
    // null apiKeyVar normalizes to "" (empty = no key, FR-006)
    assertThat(Agent.create("A", "http://x", null, "m", "p").apiKeyVar()).isEmpty();
  }

  @Test
  void snapshotCarriesConfigButNeverKeyValue() {
    Agent agent = valid();
    var snapshot = agent.toSnapshot();
    assertThat(snapshot.id()).isEqualTo(agent.id());
    assertThat(snapshot.agentName()).isEqualTo(agent.agentName());
    assertThat(snapshot.openAiCompatibleUrl()).isEqualTo(agent.openAiCompatibleUrl());
    assertThat(snapshot.apiKeyVar()).isEqualTo("MISTRAL_API_KEY");
    assertThat(snapshot.modelName()).isEqualTo(agent.modelName());
    assertThat(snapshot.personalityPrompt()).isEqualTo(agent.personalityPrompt());
  }
}
