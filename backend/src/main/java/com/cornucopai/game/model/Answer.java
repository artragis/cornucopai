package com.cornucopai.game.model;

/**
 * The card player's answer to a question: "Je sais" + answer, or "Je sais pas" (FR-010).
 *
 * Sealed interface rather than enum on purpose: {@code Knows} carries the answer
 * text as a payload, which an enum constant cannot hold per instance. The sealed
 * hierarchy keeps the two cases exhaustive for pattern matching (switch/instanceof)
 * and forbids invalid states like a hypothetical KNOWS-with-null-answer.
 */
public sealed interface Answer {
    record Knows(String answer) implements Answer {}
    record DoesNotKnow() implements Answer {}
}
