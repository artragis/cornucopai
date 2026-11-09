package com.cornucopai.game.llm.dto;

/**
 * LLM output for the validation vote (R-027). {@code vote} ∈ {@code YES}, {@code NO};
 * anything else falls back to {@code NO} (R-030).
 */
public record VoteDto(String vote) {
}
