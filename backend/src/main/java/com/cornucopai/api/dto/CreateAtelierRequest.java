package com.cornucopai.api.dto;

/** POST /api/ateliers request body (contracts/ateliers-api.yaml). */
public record CreateAtelierRequest(Integer playerCount, String featureHtml) {}
