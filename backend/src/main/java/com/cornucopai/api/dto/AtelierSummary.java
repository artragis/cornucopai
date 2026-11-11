package com.cornucopai.api.dto;

import java.time.Instant;

/**
 * GET /api/ateliers list item. {@code hasProjectFacts} (spec 007, API v0.7.0, R-056)
 * is true when the atelier has at least one persisted project fact — used by the
 * frontend dropdown that offers to inherit facts from a previous atelier (FR-007).
 */
public record AtelierSummary(String id, Instant createdAt, int playerCount, boolean hasProjectFacts) {}
