package com.cornucopai.game.agent;

import com.cornucopai.game.PlayerAgent;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.Reaction;
import com.cornucopai.game.model.Vote;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfAgentTest {

    private final PlayerAgent prof = new ProfAgent("Prof");
    private final Leg leg = new Leg("Carte-Placeholder-1", "scenario", "explanation");

    private Participant of(Archetype archetype) {
        return new Participant("X", archetype);
    }

    @Test
    void hasProfArchetype() {
        assertThat(prof.archetype()).isEqualTo(Archetype.PROF);
    }

    @Test
    void givesShortPrecisionToHappy() {
        Reaction reaction = prof.react(leg, of(Archetype.HAPPY), "<p>f</p>", true);
        assertThat(reaction).isInstanceOf(Reaction.Precision.class);
        assertThat(((Reaction.Precision) reaction).text().length()).isLessThan(100);
    }

    @Test
    void asksAQuestionBeforePrecisingAgainstGrumpy() {
        assertThat(prof.react(leg, of(Archetype.GRUMPY), "<p>f</p>", true))
                .isInstanceOf(Reaction.Question.class);
        // after a "Je sais pas", Prof gives a short precision instead of a new question
        Reaction reAsked = prof.react(leg, of(Archetype.GRUMPY), "<p>f</p>", false);
        assertThat(reAsked).isInstanceOf(Reaction.Precision.class);
        assertThat(((Reaction.Precision) reAsked).text().length()).isLessThan(100);
    }

    @Test
    void passesAgainstGreedy() {
        assertThat(prof.react(leg, of(Archetype.GREEDY), "<p>f</p>", true))
                .isInstanceOf(Reaction.Pass.class);
    }

    @Test
    void votesNoToGrumpyAndYesToOthers() {
        assertThat(prof.vote(leg, of(Archetype.GRUMPY))).isEqualTo(Vote.NO);
        assertThat(prof.vote(leg, of(Archetype.HAPPY))).isEqualTo(Vote.YES);
        assertThat(prof.vote(leg, of(Archetype.GREEDY))).isEqualTo(Vote.YES);
        assertThat(prof.vote(leg, of(Archetype.PROF))).isEqualTo(Vote.YES);
    }
}
