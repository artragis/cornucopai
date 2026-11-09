package com.cornucopai.game;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Atelier;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-3 / FR-013: the game stops exactly when every hand is empty — one leg
 * per card in the deck, no extra turn, and the atelier is marked finished.
 */
class GameEndTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private AtelierStore store() {
        return new AtelierStore(tempDir, mapper);
    }

    private Atelier newAtelier(String id, List<Participant> players, List<Card> deck) {
        return new Atelier(id, Instant.parse("2026-11-07T17:30:00Z"), "<p>feature</p>",
                players.size(), players, List.of(), deck);
    }

    private List<Card> cards(String... ids) {
        return java.util.Arrays.stream(ids)
                .map(id -> new Card(id, id, "test", "T", "Test suit", "en", "desc for " + id, null, null))
                .toList();
    }

    private GameMaster gameMaster(AtelierStore store) {
        return new GameMaster(store, new Random(7));
    }

    @Test
    void twoPlayersEightCardsProduceExactlyEightLegsAndAFinalizedAtelier() throws Exception {
        AtelierStore store = store();
        String id = store.createAtelierDir(store.newAtelierId());
        List<Participant> players = List.of(
                new Participant("Joueur-1", Archetype.GRUMPY), new Participant("Joueur-2", Archetype.HAPPY));
        List<Card> deck = cards("c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8");
        List<PlayerAgent> agents = List.of(
                new com.cornucopai.game.agent.GrumpyAgent("Joueur-1"),
                new com.cornucopai.game.agent.HappyAgent("Joueur-2"));

        Atelier result = gameMaster(store).runGame(newAtelier(id, players, deck), agents);

        // exactly one leg per card, no extra turn
        List<Leg> legs = store.loadLegs(id);
        assertThat(legs).hasSize(8);
        assertThat(legs).extracting(Leg::cardName)
                .containsExactlyInAnyOrder("c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8");

        // atelier marked finished and persisted with finishedAt
        assertThat(result.finishedAt()).isNotNull();
        JsonNode atelierJson = mapper.readTree(tempDir.resolve(id).resolve("atelier.json").toFile());
        assertThat(atelierJson.has("finishedAt")).isTrue();
        assertThat(atelierJson.get("finishedAt").asText()).isNotBlank();
    }

    @Test
    void unevenHandsStillEndExactlyWhenAllCardsArePlayed() {
        AtelierStore store = store();
        String id = store.createAtelierDir(store.newAtelierId());
        List<Participant> players = List.of(
                new Participant("A", Archetype.HAPPY),
                new Participant("B", Archetype.HAPPY),
                new Participant("C", Archetype.HAPPY));
        List<Card> deck = cards("c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8"); // hands: 3,3,2
        List<PlayerAgent> agents = List.of(
                new com.cornucopai.game.agent.HappyAgent("A"),
                new com.cornucopai.game.agent.HappyAgent("B"),
                new com.cornucopai.game.agent.HappyAgent("C"));

        gameMaster(store).runGame(newAtelier(id, players, deck), agents);

        assertThat(store.loadLegs(id)).hasSize(8);
    }
}
