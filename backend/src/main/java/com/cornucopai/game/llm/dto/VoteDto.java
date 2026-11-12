package com.cornucopai.game.llm.dto;

import com.cornucopai.game.model.ApplicabilityCheck;
import com.cornucopai.game.model.ExplanationCheck;
import com.cornucopai.game.model.VoteChoice;

/**
 * LLM output for the validation vote (R-027, spec 007 US3 / R-051). Enum-typed so the model's JSON
 * maps directly and no string validation is needed; an unmapped value fails deserialization and
 * falls back to NO (R-030).
 */
public record VoteDto(
    VoteChoice vote,
    ExplanationCheck explanationCheck,
    ApplicabilityCheck applicabilityCheck,
    String justification) {

  /** Backward-compatible constructor (legacy callers/tests). */
  public VoteDto(VoteChoice vote) {
    this(vote, ExplanationCheck.OK, ApplicabilityCheck.OK, "");
  }
}
