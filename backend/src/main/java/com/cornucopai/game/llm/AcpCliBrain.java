package com.cornucopai.game.llm;

import com.cornucopai.game.llm.dto.AnswerDto;
import com.cornucopai.game.llm.dto.PlayedCardDto;
import com.cornucopai.game.llm.dto.ReactionDto;
import com.cornucopai.game.llm.dto.VoteDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * {@link LlmBrain} for ACP providers ({@code github-copilot}, {@code mistral-vibe}): a one-shot
 * invocation of the provider CLI as a subprocess, with the full prompt (system + user) as argument
 * (R-029). The CLI's stdout is parsed as the DTO JSON (markdown fences stripped).
 *
 * <p>Documented limitation (spec assumption): ACP CLI agents get NO LangChain4j toolbox and NO
 * memory this increment — the tool loop and memory live in our orchestration; the card info they
 * need is inlined in the user prompt by {@code LlmPlayerAgent}. Any failure throws, and the agent
 * applies the fallback (R-030).
 */
public class AcpCliBrain implements LlmBrain {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(120);

  private final String command;
  private final String agentName;
  private final String systemPrompt;

  public AcpCliBrain(AgentLlmConfig config, String agentName, String systemPrompt) {
    String command = config.command();
    if (command == null || command.isBlank()) {
      command =
          switch (config.acpProvider()) {
            case "github-copilot" -> "copilot";
            case "mistral-vibe" -> "vibe";
            default -> config.acpProvider();
          };
    }
    this.command = command;
    this.agentName = agentName;
    this.systemPrompt = systemPrompt;
  }

  public String command() {
    return command;
  }

  @Override
  public PlayedCardDto playCard(String userPrompt) {
    return call(userPrompt, PlayedCardDto.class);
  }

  @Override
  public ReactionDto react(String userPrompt) {
    return call(userPrompt, ReactionDto.class);
  }

  @Override
  public AnswerDto answerQuestion(String userPrompt) {
    return call(userPrompt, AnswerDto.class);
  }

  @Override
  public VoteDto vote(String userPrompt) {
    return call(userPrompt, VoteDto.class);
  }

  private <T> T call(String userPrompt, Class<T> type) {
    String fullPrompt = systemPrompt + "\n\n" + userPrompt;
    Process process;
    try {
      process = new ProcessBuilder(command, "-p", fullPrompt).start();
    } catch (IOException e) {
      throw new RuntimeException(
          "ACP command not runnable: " + command + " (agent " + agentName + ")", e);
    }
    String stdout;
    try {
      stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      boolean finished = process.waitFor(PROCESS_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        throw new RuntimeException(
            "ACP command timed out: " + command + " (agent " + agentName + ")");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("ACP command interrupted: " + command, e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    if (process.exitValue() != 0) {
      throw new RuntimeException(
          "ACP command failed: "
              + command
              + " exit="
              + process.exitValue()
              + " (agent "
              + agentName
              + ")");
    }
    try {
      return MAPPER.readValue(JsonAnswers.stripToJson(stdout), type);
    } catch (IOException e) {
      throw new RuntimeException(
          "ACP command returned unparseable output (agent " + agentName + "): " + stdout, e);
    }
  }
}
