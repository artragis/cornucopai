package com.cornucopai.api.dto;

import java.util.List;

/**
 * POST /api/ateliers request body (contracts/ateliers-api.yaml v0.8.0, spec 009). {@code agentIds}
 * replaces the old {@code playerCount}/{@code agents} pair (FR-007): one entry per participant,
 * each the id of a {@code /api/agents} agent; duplicates allowed (FR-009), minimum 2 (FR-010).
 * {@code language} defaults to {@code fr}; {@code families} is at least one {@code setId:familyId}
 * pair. {@code inheritFactsFrom} (spec 007, v0.7.0) is unchanged.
 */
public record CreateAtelierRequest(
    List<String> agentIds,
    String featureHtml,
    String language,
    List<String> families,
    String inheritFactsFrom) {}
