package com.cornucopai.game.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.cornucopai.game.llm.FakeLlmBrain;
import com.cornucopai.game.llm.LegContext;
import com.cornucopai.game.llm.dto.VoteDto;
import com.cornucopai.game.model.ApplicabilityCheck;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.ExplanationCheck;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.VoteChoice;
import com.cornucopai.game.model.VoteDecision;
import org.junit.jupiter.api.Test;

/**
 * T036 (spec 007 US3): the vote maps the extended DTO to a structured VoteDecision with the
 * decision-tree justification; LLM failure falls back to NO + reason (FR-009/FR-010, SC-004/SC-005,
 * R-051).
 */
class VoteDecisionTreeTest {

  /** Scripted brain returning a NOT_APPLICABLE / NO vote. */
  static class NoBrain extends FakeLlmBrain {
    @Override
    public VoteDto vote(String userPrompt) {
      lastVotePrompt = userPrompt;
      return new VoteDto(
          VoteChoice.NO,
          ExplanationCheck.OK,
          ApplicabilityCheck.NOT_APPLICABLE,
          "pas d'API REST dans le projet");
    }
  }

  static class FailingBrain extends FakeLlmBrain {
    @Override
    public VoteDto vote(String userPrompt) {
      throw new RuntimeException("LLM down");
    }
  }

  @Test
  void voteWithDecisionCarriesTheStructuredJustification() {
    LlmPlayerAgent agent =
        new LlmPlayerAgent("Joueur-2", Archetype.GRUMPY, new NoBrain(), new LegContext("atelierX"));
    Leg leg = new Leg("VE2", "scénario attaque API REST", "explication");
    VoteDecision d = agent.voteWithDecision(leg, new Participant("Joueur-1", Archetype.HAPPY));
    assertThat(d.voter()).isEqualTo("Joueur-2");
    assertThat(d.vote()).isEqualTo(com.cornucopai.game.model.VoteChoice.NO);
    assertThat(d.explanationCheck()).isEqualTo(com.cornucopai.game.model.ExplanationCheck.OK);
    assertThat(d.applicabilityCheck())
        .isEqualTo(com.cornucopai.game.model.ApplicabilityCheck.NOT_APPLICABLE);
    assertThat(d.justification()).contains("API REST");
    // vote() stays compatible with the enum contract
    assertThat(agent.vote(leg, new Participant("Joueur-1", Archetype.HAPPY)))
        .isEqualTo(com.cornucopai.game.model.Vote.NO);
  }

  @Test
  void llmFailureFallsBackToNoWithReason() {
    LlmPlayerAgent agent =
        new LlmPlayerAgent(
            "Joueur-1", Archetype.HAPPY, new FailingBrain(), new LegContext("atelierX"));
    Leg leg = new Leg("VE2", "s", "e");
    VoteDecision d = agent.voteWithDecision(leg, new Participant("Joueur-2", Archetype.GRUMPY));
    assertThat(d.vote()).isEqualTo(com.cornucopai.game.model.VoteChoice.NO);
    assertThat(d.justification()).isEqualTo("repli LLM indisponible");
  }

  @Test
  void votePromptContainsBothDecisionTreeBranches() {
    String p =
        LlmPlayerAgent.votePrompt(new Leg("VE2", "s", "e"), new Participant("J", Archetype.PROF));
    assertThat(p).contains("suffisamment bien expliqué").contains("applicable à notre projet réel");
  }
}
