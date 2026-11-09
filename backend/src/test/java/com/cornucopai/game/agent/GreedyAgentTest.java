package com.cornucopai.game.agent;

import com.cornucopai.game.PlayerAgent;
import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.PlayedCard;
import com.cornucopai.game.model.Reaction;
import com.cornucopai.game.model.Vote;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GreedyAgentTest {

    private final PlayerAgent greedy = new GreedyAgent("Greedy");
    private final Leg leg = new Leg("VE2", "scenario", "explanation");
    private final Card card = new Card("VE2", "2", "webapp", "VE", "DATA VALIDATION & ENCODING", "en",
            "Robert can input malicious data because the allowed protocol format is not being checked", null, null);

    @Test
    void hasGreedyArchetype() {
        assertThat(greedy.archetype()).isEqualTo(Archetype.GREEDY);
    }

    @Test
    void givesAnExcessivelyLongExplanation() {
        PlayedCard played = greedy.playCard(card, "<p>f</p>");
        assertThat(played.cardName()).isEqualTo("VE2");
        assertThat(played.explanation()).contains("VE2");
        assertThat(played.explanation().length()).isGreaterThan(500);
        // much longer than a normal explanation
        PlayedCard normal = new HappyAgent("Happy").playCard(card, "<p>f</p>");
        assertThat(played.explanation().length()).isGreaterThan(normal.explanation().length() * 10);
    }

    @Test
    void addsExcessivelyLongPrecisions() {
        Reaction reaction = greedy.react(leg, new Participant("X", Archetype.HAPPY), "<p>f</p>", true);
        assertThat(reaction).isInstanceOf(Reaction.Precision.class);
        assertThat(((Reaction.Precision) reaction).text().length()).isGreaterThan(500);
    }

    @Test
    void votesYesForHappyAndNoForGrumpy() {
        assertThat(greedy.vote(leg, new Participant("Happy", Archetype.HAPPY))).isEqualTo(Vote.YES);
        assertThat(greedy.vote(leg, new Participant("Grumpy", Archetype.GRUMPY))).isEqualTo(Vote.NO);
        assertThat(greedy.vote(leg, new Participant("Prof", Archetype.PROF))).isEqualTo(Vote.YES);
    }
}
