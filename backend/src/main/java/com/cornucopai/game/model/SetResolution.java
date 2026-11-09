package com.cornucopai.game.model;

/**
 * Per-set resolved language for a game (FR-017): the chosen language if the set
 * has it, else the {@code en} fallback — recorded on the atelier and surfaced in
 * the UI as a badge.
 */
public record SetResolution(String id, String resolvedLanguage) {
}
