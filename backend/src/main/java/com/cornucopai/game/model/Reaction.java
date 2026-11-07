package com.cornucopai.game.model;

/**
 * What a non-playing participant does during the reaction phase (FR-008).
 * Sealed interface rather than enum because {@code Precision} and {@code Question}
 * carry their text as a payload; {@code Pass} carries none. Pattern matching over
 * the sealed hierarchy stays exhaustive at compile time.
 */
public sealed interface Reaction {
    /** Adds a precision to the leg's precisions list (FR-009). */
    record Precision(String text) implements Reaction {}
    /** Asks a question to the card player (FR-010). */
    record Question(String text) implements Reaction {}
    /** Says nothing and passes the turn. */
    record Pass() implements Reaction {}
}
