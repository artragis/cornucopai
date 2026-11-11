package com.cornucopai.game.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One structured entry of a played tour (leg), spec 005 v2 schema (R-035).
 * Replaces the v1 flat list of precision strings so that questions and their
 * answers are first-class:
 *
 * <pre>
 * PRECISION: { kind, author, text }
 * QUESTION:  { kind, author, text, knows, answer? }   (answer present iff knows=true)
 * </pre>
 *
 * <p>{@code kind} is a String on purpose: an unknown kind in a persisted file is
 * tolerated on read (WARN + kept as-is) instead of failing the whole leg.
 * {@code knows}/{@code answer} are omitted from the JSON when null (QUESTION-only
 * fields), and {@link #isQuestion()} is not serialized.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LegEntry(String kind, String author, String text, Boolean knows, String answer) {

    public static final class Kind {
        public static final String PRECISION = "PRECISION";
        public static final String QUESTION = "QUESTION";

        private Kind() {
        }
    }

    public static LegEntry precision(String author, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("entry text must be non-empty");
        }
        return new LegEntry(Kind.PRECISION, author, text, null, null);
    }

    public static LegEntry question(String author, String text, boolean knows, String answer) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("question text must be non-empty");
        }
        if (knows && (answer == null || answer.isBlank())) {
            throw new IllegalArgumentException("answer must be non-empty when knows=true");
        }
        return new LegEntry(Kind.QUESTION, author, text, knows, knows ? answer : null);
    }

    @JsonIgnore
    public boolean isQuestion() {
        return Kind.QUESTION.equals(kind);
    }
}
