package com.cornucopai.game.debate;

import com.cornucopai.game.PlayerAgent;
import com.cornucopai.game.llm.LegContext;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.DebateTurn;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T046 (spec 007 US5): the debate is bounded to {@value DebateOrchestrator#MAX_ROUNDS}
 * rounds, produces one turn per voter per round, and stores them in the LegContext
 * (FR-013, R-050/R-052).
 */
class DebateOrchestratorTest {

    /** Minimal scripted agent emitting a fixed argument. */
    static class StubAgent implements PlayerAgent {
        private final String name;
        StubAgent(String name) { this.name = name; }
        @Override public String name() { return name; }
        @Override public Archetype archetype() { return Archetype.GRUMPY; }
        @Override public com.cornucopai.game.model.PlayedCard playCard(com.cornucopai.game.card.model.Card card, String featureHtml) { return null; }
        @Override public com.cornucopai.game.model.Reaction react(Leg leg, Participant cardPlayer, String featureHtml, boolean questionsAllowed) { return new com.cornucopai.game.model.Reaction.Pass(); }
        @Override public com.cornucopai.game.model.Answer answerQuestion(String question) { return new com.cornucopai.game.model.Answer.DoesNotKnow(); }
        @Override public com.cornucopai.game.model.Vote vote(Leg leg, Participant cardPlayer) { return com.cornucopai.game.model.Vote.NO; }
        @Override public String debateArgument(Leg leg, Participant cardPlayer) { return "argument de " + name; }
    }

    @Test
    void debateIsBoundedAndStoredInContext() {
        LegContext ctx = new LegContext("atelierX");
        Leg leg = new Leg("VE2", "s", "e");
        Participant cardPlayer = new Participant("Joueur-1", Archetype.HAPPY);
        List<PlayerAgent> voters = List.of(new StubAgent("Joueur-2"), new StubAgent("Joueur-3"));

        List<DebateTurn> turns = new DebateOrchestrator().runDebate(leg, voters, cardPlayer, ctx);

        // 2 voters × MAX_ROUNDS rounds
        assertThat(turns).hasSize(2 * DebateOrchestrator.MAX_ROUNDS);
        assertThat(ctx.debateTurns()).isEqualTo(turns);
        assertThat(turns).allSatisfy(t -> assertThat(t.argument()).startsWith("argument de "));
        assertThat(turns.stream().map(DebateTurn::round).distinct()).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void emptyWhenVoterThrows() {
        LegContext ctx = new LegContext("atelierX");
        PlayerAgent failing = new StubAgent("J2") {
            @Override public String debateArgument(Leg leg, Participant cardPlayer) { throw new RuntimeException("boom"); }
        };
        List<DebateTurn> turns = new DebateOrchestrator().runDebate(new Leg("VE2", "s", "e"),
                List.of(failing), new Participant("J1", Archetype.HAPPY), ctx);
        assertThat(turns).isEmpty();
        assertThat(ctx.debateTurns()).isEmpty();
    }
}
