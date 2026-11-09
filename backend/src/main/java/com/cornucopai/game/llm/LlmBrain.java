package com.cornucopai.game.llm;

import com.cornucopai.game.llm.dto.AnswerDto;
import com.cornucopai.game.llm.dto.PlayedCardDto;
import com.cornucopai.game.llm.dto.ReactionDto;
import com.cornucopai.game.llm.dto.VoteDto;

/**
 * The single seam between a player agent and its LLM transport (spec 004, R-029).
 * One implementation of {@code PlayerAgent} ({@code LlmPlayerAgent}) talks to this
 * interface; implementations are {@code LangChain4jBrain} (OpenAI-compatible endpoints,
 * with toolbox + memory) and {@code AcpCliBrain} (ACP CLI agents, one-shot).
 *
 * <p>Each method receives a fully-built user prompt (the agent owns prompt
 * construction, so prompts are consistent across transports) and returns a DTO
 * (R-027). Implementations may throw — the agent applies the fallback (R-030).
 */
public interface LlmBrain {

    PlayedCardDto playCard(String userPrompt);

    ReactionDto react(String userPrompt);

    AnswerDto answerQuestion(String userPrompt);

    VoteDto vote(String userPrompt);
}
