package com.cornucopai.game.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.time.Instant;

/**
 * One discovery about the analyzed project, recorded in the shared project memory
 * (spec 007, FR-006, R-053). Persisted inside {@code atelier.json} as part of
 * {@code projectFacts}.
 *
 * @param text      the fact itself (non-blank), e.g. « le projet n'expose pas d'API REST publique »
 * @param author    name of the agent that discovered/recorded the fact
 * @param turnIndex tour (leg) number where it was discovered
 * @param timestamp when it was recorded
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public record ProjectFact(String text, String author, int turnIndex, Instant timestamp) {

    public ProjectFact {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must be non-blank");
        }
        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("author must be non-blank");
        }
    }
}
