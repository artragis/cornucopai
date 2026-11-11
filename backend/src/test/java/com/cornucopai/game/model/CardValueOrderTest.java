package com.cornucopai.game.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T002 (Phase 2, spec 006): canonical card value ordering 2..10 &lt; J &lt; Q &lt; K &lt; A
 * (FR-010, R-046). "10" must rank above "2" (never alphabetical).
 */
class CardValueOrderTest {

    @Test
    void numericOrderIsNaturalNotAlphabetical() {
        assertThat(CardValueOrder.compare("2", "10")).isLessThan(0);
        assertThat(CardValueOrder.compare("10", "2")).isGreaterThan(0);
        assertThat(CardValueOrder.compare("9", "10")).isLessThan(0);
    }

    @Test
    void fullCanonicalOrder() {
        String[] ordered = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
        for (int i = 0; i < ordered.length - 1; i++) {
            assertThat(CardValueOrder.compare(ordered[i], ordered[i + 1]))
                    .as("%s < %s", ordered[i], ordered[i + 1])
                    .isLessThan(0);
        }
    }

    @Test
    void aceIsHighest() {
        assertThat(CardValueOrder.compare("A", "K")).isGreaterThan(0);
        assertThat(CardValueOrder.compare("A", "2")).isGreaterThan(0);
    }

    @Test
    void equalValuesCompareEqual() {
        assertThat(CardValueOrder.compare("7", "7")).isZero();
    }

    @Test
    void unknownValueRanksLowest() {
        assertThat(CardValueOrder.compare("ZZ", "2")).isLessThan(0);
        assertThat(CardValueOrder.rank("ZZ")).isZero();
    }

    @Test
    void rankMapping() {
        assertThat(CardValueOrder.rank("2")).isEqualTo(2);
        assertThat(CardValueOrder.rank("10")).isEqualTo(10);
        assertThat(CardValueOrder.rank("J")).isEqualTo(11);
        assertThat(CardValueOrder.rank("Q")).isEqualTo(12);
        assertThat(CardValueOrder.rank("K")).isEqualTo(13);
        assertThat(CardValueOrder.rank("A")).isEqualTo(14);
    }
}
