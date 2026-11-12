package com.cornucopai.game.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cornucopai.game.llm.dto.PlayedCardDto;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * T021 (US2): the ACP brain shells out to the provider CLI in one-shot mode and parses JSON
 * (R-029).
 */
class AcpCliBrainTest {

  @TempDir Path tempDir;

  private AgentLlmConfig acpConfig(String command) {
    return new AgentLlmConfig(
        AgentLlmConfig.ProviderType.ACP, null, null, null, "github-copilot", command);
  }

  @Test
  void parsesJsonAnswerFromTheCli() throws Exception {
    Path script = tempDir.resolve("fake-acp.sh");
    Files.writeString(
        script,
        "#!/bin/sh\necho '{\"scenario\":\"scénario CLI\",\"explanation\":\"explication CLI\"}'\n");
    assertThat(script.toFile().setExecutable(true)).isTrue();
    AcpCliBrain brain = new AcpCliBrain(acpConfig(script.toString()), "Joueur-1", "SYS");
    PlayedCardDto dto = brain.playCard("user prompt");
    assertThat(dto.scenario()).isEqualTo("scénario CLI");
    assertThat(dto.explanation()).isEqualTo("explication CLI");
  }

  @Test
  void stripsMarkdownFencesAroundJson() throws Exception {
    Path script = tempDir.resolve("fake-acp-fenced.sh");
    Files.writeString(script, "#!/bin/sh\necho '```json'\necho '{\"vote\":\"NO\"}'\necho '```'\n");
    assertThat(script.toFile().setExecutable(true)).isTrue();
    AcpCliBrain brain = new AcpCliBrain(acpConfig(script.toString()), "Joueur-1", "SYS");
    assertThat(brain.vote("user prompt").vote()).isEqualTo(com.cornucopai.game.model.VoteChoice.NO);
  }

  @Test
  void missingCommandThrowsSoTheAgentCanFallBack() {
    AcpCliBrain brain =
        new AcpCliBrain(
            acpConfig(tempDir.resolve("no-such-binary-xyz").toString()), "Joueur-1", "SYS");
    assertThatThrownBy(() -> brain.playCard("user prompt")).isInstanceOf(RuntimeException.class);
  }

  @Test
  void defaultCommandComesFromTheProviderName() {
    // no explicit command -> the ACP provider name maps to its CLI binary
    AcpCliBrain vibe =
        new AcpCliBrain(
            new AgentLlmConfig(
                AgentLlmConfig.ProviderType.ACP, null, null, null, "mistral-vibe", null),
            "J",
            "S");
    assertThat(vibe.command()).isEqualTo("vibe");
    AcpCliBrain copilot =
        new AcpCliBrain(
            new AgentLlmConfig(
                AgentLlmConfig.ProviderType.ACP, null, null, null, "github-copilot", null),
            "J",
            "S");
    assertThat(copilot.command()).isEqualTo("copilot");
  }
}
