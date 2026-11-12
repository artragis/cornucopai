package com.cornucopai.game.llm;

import com.cornucopai.game.llm.dto.AnswerDto;
import com.cornucopai.game.llm.dto.PlayedCardDto;
import com.cornucopai.game.llm.dto.ReactionDto;
import com.cornucopai.game.llm.dto.VoteDto;
import dev.langchain4j.service.V;

/**
 * The LangChain4j AI-service interface used by {@link LangChain4jBrain} (R-021, R-027). Each method
 * takes one fully-built user prompt ({@code @V("input")}) and returns a DTO record; AiServices maps
 * the model's JSON answer onto the record (structured output). The system prompt (game rules +
 * personality + output contract) is supplied through {@code systemMessageProvider}, the per-agent
 * memory through {@code chatMemory}, and the toolbox through {@code tools(...)}.
 */
public interface PlayerAiService {

  PlayedCardDto playCard(@V("input") String input);

  ReactionDto react(@V("input") String input);

  AnswerDto answerQuestion(@V("input") String input);

  VoteDto vote(@V("input") String input);

  /** Spec 007 US5 (R-050/R-052): one free-text debate argument for the pre-vote exchange. */
  String debate(@V("input") String input);
}
