package com.cornucopai.game.card;

import com.cornucopai.game.card.model.CardSet;
import java.util.List;

/**
 * The card catalog payload (GET /api/card-catalog, contracts/card-catalog-api.yaml): all distinct
 * available languages + the sets (latest version per edition) with their families.
 */
public record CardCatalog(List<String> availableLanguages, List<CardSet> sets) {}
