package com.cornucopai.game.card.model;

/**
 * A family (suit) inside a set — the selectable unit in the game creator
 * (spec 003 clarification). Identified globally by {@code setId + ":" + id}
 * (e.g. {@code webapp:VE}).
 */
public record CardFamily(String id, String name, String setId, int cardCount) {
}
