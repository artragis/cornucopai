package com.cornucopai.api.dto;

import java.time.Instant;

/** GET /api/ateliers list item. */
public record AtelierSummary(String id, Instant createdAt, int playerCount) {}
