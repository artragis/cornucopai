package com.cornucopai.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Fixed placeholder deck for this increment (research R-005, spec FR-003: no
 * card-family choice yet). The shuffle uses an injectable Random so tests are
 * reproducible with a seed.
 */
public class PlaceholderCards {

    public static final int DECK_SIZE = 8;

    private final Random random;

    public PlaceholderCards(Random random) {
        this.random = random;
    }

    /** Returns the 8 placeholder cards in a shuffled order. */
    public List<String> shuffledDeck() {
        List<String> deck = new ArrayList<>(DECK_SIZE);
        for (int i = 1; i <= DECK_SIZE; i++) {
            deck.add("Carte-Placeholder-" + i);
        }
        Collections.shuffle(deck, random);
        return deck;
    }

    /** Deals the deck round-robin: player 1 gets deck[0], deck[N], ... */
    public Map<String, List<String>> deal(List<String> playerNames, List<String> deck) {
        Map<String, List<String>> hands = new LinkedHashMap<>();
        for (String name : playerNames) {
            hands.put(name, new ArrayList<>());
        }
        int i = 0;
        for (String card : deck) {
            hands.get(playerNames.get(i % playerNames.size())).add(card);
            i++;
        }
        return hands;
    }
}
