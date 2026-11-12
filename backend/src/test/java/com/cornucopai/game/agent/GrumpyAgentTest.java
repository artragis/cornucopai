package com.cornucopai.game.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.cornucopai.game.PlayerAgent;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.Reaction;
import com.cornucopai.game.model.Vote;
import org.junit.jupiter.api.Test;

class GrumpyAgentTest {

  private final PlayerAgent grumpy = new GrumpyAgent("Grumpy");
  private final Leg leg = new Leg("Carte-Placeholder-1", "scenario", "explanation");

  @Test
  void hasGrumpyArchetype() {
    assertThat(grumpy.archetype()).isEqualTo(Archetype.GRUMPY);
    assertThat(grumpy.name()).isEqualTo("Grumpy");
  }

  @Test
  void alwaysVotesNo() {
    assertThat(grumpy.vote(leg, new Participant("X", Archetype.HAPPY))).isEqualTo(Vote.NO);
    assertThat(grumpy.vote(leg, new Participant("X", Archetype.GREEDY))).isEqualTo(Vote.NO);
    assertThat(grumpy.vote(leg, new Participant("X", Archetype.PROF))).isEqualTo(Vote.NO);
  }

  @Test
  void alwaysAsksQuestionBeforePassing() {
    Reaction reaction = grumpy.react(leg, new Participant("X", Archetype.HAPPY), "<p>f</p>", true);
    assertThat(reaction).isInstanceOf(Reaction.Question.class);
  }

  @Test
  void passesWhenQuestionsAreNoLongerAllowed() {
    Reaction reaction = grumpy.react(leg, new Participant("X", Archetype.HAPPY), "<p>f</p>", false);
    assertThat(reaction).isInstanceOf(Reaction.Pass.class);
  }
}
