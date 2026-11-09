package com.cornucopai.game.llm.dto;

/**
 * LLM output for "play a card" (R-027). {@code cardName} is NEVER taken from the LLM:
 * the agent sets it from the dealt card.
 */
public record PlayedCardDto(String scenario, String explanation) {
}
