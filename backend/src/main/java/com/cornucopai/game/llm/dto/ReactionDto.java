package com.cornucopai.game.llm.dto;

/**
 * LLM output for the reaction phase (R-027). {@code type} ∈ {@code PRECISION}, {@code QUESTION},
 * {@code PASS}; {@code text} carries the payload for the first two.
 */
public record ReactionDto(String type, String text) {}
