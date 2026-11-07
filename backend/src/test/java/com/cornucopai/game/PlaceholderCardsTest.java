package com.cornucopai.game;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceholderCardsTest {

    @Test
    void deckHasEightPlaceholderCards() {
        List<String> deck = new PlaceholderCards(new Random(42)).shuffledDeck();
        assertThat(deck).hasSize(8).containsExactlyInAnyOrder(
                "Carte-Placeholder-1", "Carte-Placeholder-2", "Carte-Placeholder-3", "Carte-Placeholder-4",
                "Carte-Placeholder-5", "Carte-Placeholder-6", "Carte-Placeholder-7", "Carte-Placeholder-8");
    }

    @Test
    void shuffleIsDeterministicForAGivenSeed() {
        List<String> first = new PlaceholderCards(new Random(42)).shuffledDeck();
        List<String> second = new PlaceholderCards(new Random(42)).shuffledDeck();
        assertThat(first).isEqualTo(second);
    }

    @Test
    void dealIsRoundRobin() {
        List<String> deck = List.of("c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8");
        Map<String, List<String>> hands =
                new PlaceholderCards(new Random(1)).deal(List.of("Joueur-1", "Joueur-2", "Joueur-3", "Joueur-4"), deck);
        assertThat(hands).containsOnlyKeys("Joueur-1", "Joueur-2", "Joueur-3", "Joueur-4");
        assertThat(hands.get("Joueur-1")).containsExactly("c1", "c5");
        assertThat(hands.get("Joueur-2")).containsExactly("c2", "c6");
        assertThat(hands.get("Joueur-3")).containsExactly("c3", "c7");
        assertThat(hands.get("Joueur-4")).containsExactly("c4", "c8");
    }
}
