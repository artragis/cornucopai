package com.cornucopai.game.llm;

import com.cornucopai.game.llm.dto.AnswerDto;
import com.cornucopai.game.llm.dto.PlayedCardDto;
import com.cornucopai.game.llm.dto.ReactionDto;
import com.cornucopai.game.llm.dto.VoteDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** T007: the scripted test double returns canned DTOs and captures prompts (R-031). */
class FakeLlmBrainTest {

    private final FakeLlmBrain brain = new FakeLlmBrain();

    @Test
    void returnsCannedDtosAndCapturesPrompts() {
        PlayedCardDto played = brain.playCard("prompt-play");
        assertThat(played.scenario()).isNotBlank();
        assertThat(played.explanation()).isNotBlank();
        assertThat(brain.lastPlayPrompt).isEqualTo("prompt-play");

        ReactionDto reaction = brain.react("prompt-react");
        assertThat(reaction.type()).isEqualTo("PRECISION");
        assertThat(reaction.text()).isNotBlank();
        assertThat(brain.lastReactPrompt).isEqualTo("prompt-react");

        AnswerDto answer = brain.answerQuestion("prompt-answer");
        assertThat(answer.knows()).isTrue();
        assertThat(answer.answer()).isNotBlank();
        assertThat(brain.lastAnswerPrompt).isEqualTo("prompt-answer");

        VoteDto vote = brain.vote("prompt-vote");
        assertThat(vote.vote()).isEqualTo("YES");
        assertThat(brain.lastVotePrompt).isEqualTo("prompt-vote");
    }
}
