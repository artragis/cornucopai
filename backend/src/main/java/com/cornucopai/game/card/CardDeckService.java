package com.cornucopai.game.card;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.model.SetResolution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Builds the real combined deck from the selected families (FR-007/FR-008): groups families by set,
 * resolves each set's language (chosen lang, else {@code en} within the latest version —
 * R-015/FR-016), reads the real cards, records the per-set resolved language (FR-017), dedupes
 * families (FR-013), shuffles with the injectable {@link Random}. Dealing round-robin is done by
 * {@code GameMaster}.
 */
@ApplicationScoped
public class CardDeckService {

  private final CardCatalogService catalog;

  @Inject
  public CardDeckService(CardCatalogService catalog) {
    this.catalog = catalog;
  }

  /** Test-friendly constructor. */
  public CardDeckService(CardCatalogService catalog, Random ignored) {
    this.catalog = catalog;
  }

  public DeckResult buildDeck(List<String> selectedFamilies, String language, Random random) {
    String lang = language == null || language.isBlank() ? "fr" : language.toLowerCase();
    // group family ids by set, deduped, preserving order (FR-013)
    Map<String, Set<String>> bySet = new LinkedHashMap<>();
    Set<String> seen = new LinkedHashSet<>();
    for (String family : selectedFamilies) {
      if (family == null) {
        continue;
      }
      String[] parts = family.split(":", 2);
      if (parts.length != 2) {
        continue;
      }
      if (!seen.add(family)) {
        continue; // no duplicate family
      }
      bySet.computeIfAbsent(parts[0], k -> new LinkedHashSet<>()).add(parts[1]);
    }

    List<Card> deck = new ArrayList<>();
    List<SetResolution> sets = new ArrayList<>();
    for (Map.Entry<String, Set<String>> e : bySet.entrySet()) {
      String setId = e.getKey();
      Set<String> familyIds = e.getValue();
      String resolved = catalog.resolveLanguage(setId, lang).orElse("en");
      sets.add(new SetResolution(setId, resolved));
      deck.addAll(catalog.cardsFor(setId, familyIds, resolved));
    }
    Collections.shuffle(deck, random);
    return new DeckResult(deck, sets);
  }

  /** Result of {@link #buildDeck}: the shuffled real deck + per-set resolved language. */
  public record DeckResult(List<Card> deck, List<SetResolution> sets) {}
}
