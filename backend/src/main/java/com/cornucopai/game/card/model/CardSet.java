package com.cornucopai.game.card.model;

import java.util.List;

/**
 * A Cornucopia card set (edition), latest version only (R-013). Exposed by the
 * catalog endpoint to feed the hierarchical set → familles selector.
 */
public record CardSet(String id, String version, List<String> languages,
                      List<CardFamily> families, int totalCards) {
}
