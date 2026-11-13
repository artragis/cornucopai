package com.cornucopai.game.llm;

import com.cornucopai.game.llm.dto.AnswerDto;
import com.cornucopai.game.llm.dto.PlayedCardDto;
import com.cornucopai.game.llm.dto.ReactionDto;
import com.cornucopai.game.llm.dto.VoteDto;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.CreatedAware;

/**
 * The LangChain4j AI-service interface used by {@link LangChain4jBrain} (R-021, R-027). Each method
 * takes one fully-built user prompt ({@code @V("input")}) and returns a DTO record; AiServices maps
 * the model's JSON answer onto the record (structured output). The system prompt (game rules +
 * personality + output contract) is supplied through {@code systemMessageProvider}, the per-agent
 * memory through {@code chatMemory}, and the toolbox through {@code tools(...)}.
 *
 * <p>{@code @CreatedAware} (R-065, observed 2026-11-11 on a real run): inside Quarkus, {@code
 * AiServices.builder(...).build()} goes through {@code QuarkusAiServicesFactory}, which requires
 * the interface to be registered at build time — without this annotation every build() failed with
 * « Quarkus was not able to determine class ... as an AiService at build time ».
 */
@CreatedAware
public interface PlayerAiService {

  PlayedCardDto playCard(@V("input") String input);

  ReactionDto react(@V("input") String input);

  AnswerDto answerQuestion(@V("input") String input);

  VoteDto vote(@V("input") String input);

  /** Spec 007 US5 (R-050/R-052): one free-text debate argument for the pre-vote exchange. */
  String debate(@V("input") String input);
}
