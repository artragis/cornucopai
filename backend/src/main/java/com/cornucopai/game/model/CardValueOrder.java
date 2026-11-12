package com.cornucopai.game.model;

import java.util.Map;
import org.jboss.logging.Logger;

/**
 * Canonical Cornucopia card-value ordering (spec 006 FR-010, R-046): 2 &lt; 3 &lt; … &lt; 10 &lt; J
 * &lt; Q &lt; K &lt; A. Comparison is by rank, never alphabetical ("10" must rank above "2").
 * Unknown values rank 0 (lowest) and log a WARN.
 */
public final class CardValueOrder {

  private static final Logger LOG = Logger.getLogger(CardValueOrder.class);

  private static final Map<String, Integer> RANKS =
      Map.ofEntries(
          Map.entry("2", 2),
          Map.entry("3", 3),
          Map.entry("4", 4),
          Map.entry("5", 5),
          Map.entry("6", 6),
          Map.entry("7", 7),
          Map.entry("8", 8),
          Map.entry("9", 9),
          Map.entry("10", 10),
          Map.entry("J", 11),
          Map.entry("Q", 12),
          Map.entry("K", 13),
          Map.entry("A", 14));

  private CardValueOrder() {}

  /** Rank of a card value; unknown values return 0. */
  public static int rank(String value) {
    if (value == null) {
      return 0;
    }
    Integer r = RANKS.get(value);
    if (r == null) {
      LOG.warnf("Unknown card value '%s' — ranking 0 (lowest)", value);
      return 0;
    }
    return r;
  }

  /** Compare two card values by canonical rank. */
  public static int compare(String a, String b) {
    return Integer.compare(rank(a), rank(b));
  }
}
