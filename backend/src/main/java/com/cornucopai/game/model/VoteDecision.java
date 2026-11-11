package com.cornucopai.game.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * The structured result of one agent's vote on a leg, following the two-branch
 * decision tree of spec 007 (FR-009/FR-010, R-051). Persisted in the leg file's
 * {@code votes} array (leg-schema.json v4.0.0).
 *
 * <p>Enum-typed fields (no string validation needed — Jackson maps enums directly).
 *
 * @param voter               name of the voting agent
 * @param vote                {@link VoteChoice#YES} or {@link VoteChoice#NO}
 * @param explanationCheck    branch (a)
 * @param applicabilityCheck  branch (b)
 * @param justification       short free-text reason
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public record VoteDecision(String voter, VoteChoice vote, ExplanationCheck explanationCheck,
                           ApplicabilityCheck applicabilityCheck, String justification) {

    public VoteDecision {
        if (voter == null || voter.isBlank()) {
            throw new IllegalArgumentException("voter must be non-blank");
        }
        if (vote == null || explanationCheck == null || applicabilityCheck == null) {
            throw new IllegalArgumentException("vote, explanationCheck and applicabilityCheck are required");
        }
    }
}
