package com.cornucopai.game.agent;

import com.cornucopai.game.PlayerAgent;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.Reaction;
import com.cornucopai.game.model.Vote;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HappyAgentTest {

    private final PlayerAgent happy = new HappyAgent("Happy");
    private final Leg leg = new Leg("Carte-Placeholder-1", "scenario", "explanation");

    @Test
    void hasHappyArchetype() {
        assertThat(happy.archetype()).isEqualTo(Archetype.HAPPY);
    }

    @Test
    void alwaysVotesYes() {
        assertThat(happy.vote(leg, new Participant("X", Archetype.GRUMPY))).isEqualTo(Vote.YES);
        assertThat(happy.vote(leg, new Participant("X", Archetype.GREEDY))).isEqualTo(Vote.YES);
        assertThat(happy.vote(leg, new Participant("X", Archetype.PROF))).isEqualTo(Vote.YES);
    }

    @Test
    void neverAsksQuestionsNorGivesPrecisions() {
        assertThat(happy.react(leg, new Participant("X", Archetype.HAPPY), "<p>f</p>", true))
                .isInstanceOf(Reaction.Pass.class);
        assertThat(happy.react(leg, new Participant("X", Archetype.GRUMPY), "<p>f</p>", true))
                .isInstanceOf(Reaction.Pass.class);
        assertThat(happy.react(leg, new Participant("X", Archetype.GREEDY), "<p>f</p>", false))
                .isInstanceOf(Reaction.Pass.class);
    }
}
