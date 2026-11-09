package com.cornucopai.api.dto;

import java.util.List;

/**
 * POST /api/ateliers request body (contracts/ateliers-api.yaml v0.4.0).
 * {@code language} defaults to {@code fr} when absent (spec 003 FR-015); {@code families}
 * is the list of selected {@code setId:familyId} pairs (FR-005), at least one (FR-006).
 * {@code agents} (spec 004) is the OPTIONAL per-player LLM provider config; when absent,
 * every player uses the server defaults (FR-002).
 */
public record CreateAtelierRequest(Integer playerCount, String featureHtml, String language,
                                   List<String> families, List<AgentConfigDto> agents) {
}
