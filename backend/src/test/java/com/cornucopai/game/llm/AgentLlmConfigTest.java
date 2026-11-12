package com.cornucopai.game.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AgentLlmConfigTest {

  @Test
  void openAiCompatibleRequiresNonBlankBaseUrl() {
    assertThatThrownBy(() ->
            new AgentLlmConfig(
                AgentLlmConfig.ProviderType.OPENAI_COMPATIBLE, null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("baseUrl");
    assertThatThrownBy(() ->
            new AgentLlmConfig(
                AgentLlmConfig.ProviderType.OPENAI_COMPATIBLE, "  ", null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("baseUrl");
  }

  @Test
  void acpRequiresKnownProvider() {
    assertThatThrownBy(
            () -> new AgentLlmConfig(AgentLlmConfig.ProviderType.ACP, null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("acpProvider");
    assertThatThrownBy(() ->
            new AgentLlmConfig(
                AgentLlmConfig.ProviderType.ACP, null, null, null, "some-other-agent", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("acpProvider");
    AgentLlmConfig copilot =
        new AgentLlmConfig(
            AgentLlmConfig.ProviderType.ACP, null, null, null, "github-copilot", null);
    assertThat(copilot.acpProvider()).isEqualTo("github-copilot");
    AgentLlmConfig vibe =
        new AgentLlmConfig(
            AgentLlmConfig.ProviderType.ACP, null, null, null, "mistral-vibe", "vibe");
    assertThat(vibe.command()).isEqualTo("vibe");
  }

  @Test
  void providerTypeIsRequired() {
    assertThatThrownBy(() -> new AgentLlmConfig(null, "http://x", null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("providerType");
  }

  @Test
  void defaultOpenAiCompatibleCarriesBaseUrlAndNoKey() {
    AgentLlmConfig cfg = AgentLlmConfig.defaultOpenAiCompatible("http://localhost:1235/v1");
    assertThat(cfg.providerType()).isEqualTo(AgentLlmConfig.ProviderType.OPENAI_COMPATIBLE);
    assertThat(cfg.baseUrl()).isEqualTo("http://localhost:1235/v1");
    assertThat(cfg.apiKey()).isNull();
    assertThat(cfg.modelName()).isNull();
  }

  @Test
  void parseProviderTypeIsCaseInsensitiveAndRejectsUnknown() {
    assertThat(AgentLlmConfig.parseProviderType("OPENAI_COMPATIBLE"))
        .isEqualTo(AgentLlmConfig.ProviderType.OPENAI_COMPATIBLE);
    assertThat(AgentLlmConfig.parseProviderType("acp")).isEqualTo(AgentLlmConfig.ProviderType.ACP);
    assertThatThrownBy(() -> AgentLlmConfig.parseProviderType("FOO"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unknown providerType");
    assertThatThrownBy(() -> AgentLlmConfig.parseProviderType(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("providerType");
  }
}
