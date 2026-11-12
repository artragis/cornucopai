package com.cornucopai.game.llm.dto;

/**
 * LLM output for a question ("Je sais" / "Je sais pas", FR-010, R-027). {@code knows=false} → "Je
 * sais pas"; {@code knows=true} with a blank answer is treated as "Je sais pas" by the agent.
 */
public record AnswerDto(Boolean knows, String answer) {}
