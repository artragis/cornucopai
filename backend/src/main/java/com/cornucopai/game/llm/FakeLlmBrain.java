package com.cornucopai.game.llm;

import com.cornucopai.game.llm.dto.AnswerDto;
import com.cornucopai.game.llm.dto.PlayedCardDto;
import com.cornucopai.game.llm.dto.ReactionDto;
import com.cornucopai.game.llm.dto.VoteDto;

/**
 * Scripted {@link LlmBrain} test double (R-031). It is ONLY used when the property
 * {@code cornucopai.llm.brain=fake} is set (the test profile sets it, so CI never
 * calls a real LLM); production runs with the default {@code auto}. It returns canned
 * DTOs and captures the prompts it receives so tests can assert on prompt content
 * (e.g. the prompt-injection hardening test, T045).
 */
public class FakeLlmBrain implements LlmBrain {

    public volatile String lastPlayPrompt;
    public volatile String lastReactPrompt;
    public volatile String lastAnswerPrompt;
    public volatile String lastVotePrompt;

    @Override
    public PlayedCardDto playCard(String userPrompt) {
        lastPlayPrompt = userPrompt;
        return new PlayedCardDto("Scénario de test", "Explication de test");
    }

    @Override
    public ReactionDto react(String userPrompt) {
        lastReactPrompt = userPrompt;
        return new ReactionDto("PRECISION", "Précision de test");
    }

    @Override
    public AnswerDto answerQuestion(String userPrompt) {
        lastAnswerPrompt = userPrompt;
        return new AnswerDto(true, "Réponse de test");
    }

    @Override
    public VoteDto vote(String userPrompt) {
        lastVotePrompt = userPrompt;
        return new VoteDto("YES");
    }
}
