package com.cornucopai.game.agent;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T022 (US4) + T024 (US5): prompt structure — conditional react statuses, minimal vote
 * prompt, and French-only terminology (no standalone word "leg" in any prompt).
 */
class PromptsTest {

    private static final Pattern LEG_WORD = Pattern.compile("\\bleg\\b", Pattern.CASE_INSENSITIVE);

    private final Card card = new Card("VE2", "2", "webapp", "VE", "DATA VALIDATION & ENCODING", "en",
            "Robert can input malicious data", null, null);
    private final Leg leg = new Leg("VE2", "scénario du tour", "explication du tour");
    private final Participant cardPlayer = new Participant("Joueur-2", Archetype.HAPPY);

    @Test
    void reactPromptWithQuestionsAllowedListsThreeStatusesAndOneQuestionRule() {
        String p = LlmPlayerAgent.reactPrompt(leg, cardPlayer, "<p>f</p>", true);
        assertThat(p).contains("PRECISION").contains("QUESTION").contains("PASS");
        assertThat(p).containsIgnoringCase("une seule question");
    }

    @Test
    void reactPromptWithoutQuestionsOffersOnlyPrecisionAndPass() {
        String p = LlmPlayerAgent.reactPrompt(leg, cardPlayer, "<p>f</p>", false);
        assertThat(p).contains("PRECISION").contains("PASS");
        assertThat(p).containsIgnoringCase("tu ne peux pas poser de question");
        // QUESTION must not appear as a proposed status at all in this variant
        assertThat(p).doesNotContain("QUESTION");
    }

    @Test
    void votePromptDemandsBareJsonAndForbidsReasoning() {
        String p = LlmPlayerAgent.votePrompt(leg, cardPlayer);
        assertThat(p).contains("{\"vote\": \"YES\"}").contains("{\"vote\": \"NO\"}");
        assertThat(p).containsIgnoringCase("aucun raisonnement");
        assertThat(p).containsIgnoringCase("aucune explication");
    }

    @Test
    void noPromptContainsTheWordLeg() {
        String play = LlmPlayerAgent.playPrompt(card, "<p>f</p>");
        String reactYes = LlmPlayerAgent.reactPrompt(leg, cardPlayer, "<p>f</p>", true);
        String reactNo = LlmPlayerAgent.reactPrompt(leg, cardPlayer, "<p>f</p>", false);
        String answer = LlmPlayerAgent.answerPrompt("une question ?");
        String vote = LlmPlayerAgent.votePrompt(leg, cardPlayer);
        for (String p : new String[]{play, reactYes, reactNo, answer, vote}) {
            assertThat(LEG_WORD.matcher(p).find()).as("prompt must not contain 'leg': %s", p).isFalse();
        }
        // the current turn is referred to as « tour »
        assertThat(reactYes).contains("tour");
        assertThat(vote).contains("tour");
    }
}
