package com.cornucopai.game.model;

/** What the current player gives when their turn comes (FR-006). */
public record PlayedCard(String cardName, String scenario, String explanation) {}
