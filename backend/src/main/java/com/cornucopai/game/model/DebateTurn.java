package com.cornucopai.game.model;

/**
 * One argument in the pre-vote debate (spec 007 US5, R-052). Transient — held in
 * {@code LegContext} and injected into vote prompts, not persisted.
 *
 * @param agent    the debater's name
 * @param argument the free-text argument
 * @param round    debate round (1-based, bounded)
 */
public record DebateTurn(String agent, String argument, int round) {
}
