package com.cornucopai.game.model;

/** Lifecycle of a leg: written PENDING, then VALIDATED or REJECTED after the vote (FR-012). */
public enum ValidationStatus {
    PENDING, VALIDATED, REJECTED
}
