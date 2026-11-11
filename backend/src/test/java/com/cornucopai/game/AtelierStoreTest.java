package com.cornucopai.game;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Atelier;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.LegEntry;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.ValidationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AtelierStoreTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private AtelierStore store() {
        return new AtelierStore(tempDir, mapper);
    }

    private static Card card(String id) {
        return new Card(id, "2", "webapp", "VE", "DATA VALIDATION & ENCODING", "en", "desc for " + id, null, null);
    }

    @Test
    void atelierIdUsesDashesAndNoColons() {
        String id = store().newAtelierId();
        assertThat(id).matches("atelier\\d{8}-\\d{6}");
        assertThat(id).doesNotContain(":");
    }

    @Test
    void createAtelierDirAvoidsCollisionBySuffixing() {
        AtelierStore store = store();
        String id = store.newAtelierId();
        String first = store.createAtelierDir(id);
        String second = store.createAtelierDir(id);
        assertThat(first).isEqualTo(id);
        assertThat(second).isNotEqualTo(first);
        assertThat(Files.isDirectory(tempDir.resolve(first))).isTrue();
        assertThat(Files.isDirectory(tempDir.resolve(second))).isTrue();
    }

    @Test
    void saveAndLoadAtelierRoundTrip() {
        AtelierStore store = store();
        String id = store.createAtelierDir(store.newAtelierId());
        Atelier atelier = new Atelier(id, Instant.parse("2026-11-07T17:30:00Z"), "<p>feat</p>", 2,
                List.of(new Participant("Joueur-1", Archetype.GRUMPY),
                        new Participant("Joueur-2", Archetype.HAPPY)),
                List.of("Joueur-2", "Joueur-1"),
                List.of(card("VE2"), card("AT2")));
        store.saveAtelier(id, atelier);

        assertThat(store.loadAtelier(id)).isPresent();
        Atelier loaded = store.loadAtelier(id).get();
        assertThat(loaded.id()).isEqualTo(id);
        assertThat(loaded.featureHtml()).isEqualTo("<p>feat</p>");
        assertThat(loaded.players()).hasSize(2);
        assertThat(loaded.turnOrder()).containsExactly("Joueur-2", "Joueur-1");
    }

    @Test
    void loadAtelierUnknownIdIsEmpty() {
        assertThat(store().loadAtelier("atelier2099-01-01-000000")).isEmpty();
    }

    @Test
    void legFileHasExactlyFiveFieldsAndStartsPending() throws Exception {
        AtelierStore store = store();
        String id = store.createAtelierDir(store.newAtelierId());
        Leg leg = new Leg("Carte-Placeholder-1", "scenario", "explanation");
        store.saveLeg(id, 1, "Joueur-1", leg);

        Path file = tempDir.resolve(id).resolve("leg-001-joueur-1.json");
        assertThat(file).exists();
        JsonNode json = mapper.readTree(file.toFile());
        Iterator<String> fields = json.fieldNames();
        assertThat(fields).toIterable()
                .containsExactlyInAnyOrder("cardName", "scenario", "explanation", "entries", "validation",
                        "turnIndex", "majorSuit", "suitId", "cardValue", "points", // spec 006 v3 additive fields
                        "votes"); // spec 007 v4 additive field (R-053)
        assertThat(json.get("cardName").asText()).isEqualTo("Carte-Placeholder-1");
        assertThat(json.get("validation").asText()).isEqualTo("PENDING");
        assertThat(json.get("entries").isArray()).isTrue();
        assertThat(json.get("entries")).isEmpty();
    }

    @Test
    void legUpdateAppendsEntriesAndFlipsValidation() throws Exception {
        AtelierStore store = store();
        String id = store.createAtelierDir(store.newAtelierId());
        Leg leg = new Leg("c", "s", "e");
        store.saveLeg(id, 3, "Joueur-2", leg);

        leg.addEntry(LegEntry.precision("Joueur-1", "une précision"));
        leg.addEntry(LegEntry.question("Joueur-1", "une question ?", true, "réponse à une question"));
        leg.setValidation(ValidationStatus.VALIDATED);
        store.saveLeg(id, 3, "Joueur-2", leg);

        Path file = tempDir.resolve(id).resolve("leg-003-joueur-2.json");
        JsonNode json = mapper.readTree(file.toFile());
        JsonNode entries = json.get("entries");
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).get("kind").asText()).isEqualTo("PRECISION");
        assertThat(entries.get(0).get("author").asText()).isEqualTo("Joueur-1");
        assertThat(entries.get(1).get("kind").asText()).isEqualTo("QUESTION");
        assertThat(entries.get(1).get("knows").asBoolean()).isTrue();
        assertThat(entries.get(1).get("answer").asText()).isEqualTo("réponse à une question");
        assertThat(json.get("validation").asText()).isEqualTo("VALIDATED");
    }

    @Test
    void loadLegsReturnsAllLegFilesInTurnOrder() {
        AtelierStore store = store();
        String id = store.createAtelierDir(store.newAtelierId());
        store.saveLeg(id, 2, "Joueur-1", new Leg("c2", "s", "e"));
        store.saveLeg(id, 1, "Joueur-2", new Leg("c1", "s", "e"));

        List<Leg> legs = store.loadLegs(id);
        assertThat(legs).hasSize(2);
        assertThat(legs.get(0).cardName()).isEqualTo("c1");
        assertThat(legs.get(1).cardName()).isEqualTo("c2");
    }

    @Test
    void listAteliersIsNewestFirst() {
        AtelierStore store = store();
        String oldId = store.createAtelierDir("atelier2026-01-01-000000");
        String newId = store.createAtelierDir("atelier2026-01-02-000000");
        store.saveAtelier(oldId, new Atelier(oldId, Instant.parse("2026-01-01T00:00:00Z"), "old", 2,
                List.of(), List.of(), List.of()));
        store.saveAtelier(newId, new Atelier(newId, Instant.parse("2026-01-02T00:00:00Z"), "new", 4,
                List.of(), List.of(), List.of()));

        List<Atelier> ateliers = store.listAteliers();
        assertThat(ateliers).extracting(Atelier::id).containsExactly(newId, oldId);
    }

    @Test
    void slugLowercasesAndReplacesNonAlnum() {
        assertThat(AtelierStore.slug("Joueur-1")).isEqualTo("joueur-1");
    }

    @Test
    void legacyAtelierWithStringDeckIsSkippedNotFatal() throws Exception {
        AtelierStore store = store();
        String legacyId = store.createAtelierDir("atelier2026-11-07-100000");
        // Simulate a feature-002 atelier.json where deck was List<String>
        Files.writeString(tempDir.resolve(legacyId).resolve("atelier.json"), """
                {
                  "id": "atelier2026-11-07-100000",
                  "createdAt": "2026-11-07T10:00:00Z",
                  "featureHtml": "<p>old</p>",
                  "playerCount": 2,
                  "players": [],
                  "turnOrder": [],
                  "deck": ["Carte-Placeholder-1", "Carte-Placeholder-2"]
                }
                """);
        String goodId = store.createAtelierDir("atelier2026-11-08-100000");
        store.saveAtelier(goodId, new Atelier(goodId, Instant.parse("2026-11-08T10:00:00Z"), "new", 2,
                List.of(), List.of(), List.of(card("VE2"))));

        // loadAtelier on the legacy file returns empty instead of throwing
        assertThat(store.loadAtelier(legacyId)).isEmpty();
        // listAteliers skips the legacy file and still returns the good one
        List<Atelier> ateliers = store.listAteliers();
        assertThat(ateliers).extracting(Atelier::id).containsExactly(goodId);
    }

    @Test
    void pre006AtelierWithoutTourFieldsLoadsWithEmptyDefaults() throws Exception {
        // T032 (spec 006, FR-017): a pre-006 atelier.json (real Card deck, no tours/scores/winner)
        // and a v2 leg file (no v3 fields) must load without error and expose empty/null tour fields
        AtelierStore store = store();
        String id = store.createAtelierDir("atelier2026-11-10-000000");
        Files.writeString(tempDir.resolve(id).resolve("atelier.json"), """
                {
                  "id": "atelier2026-11-10-000000",
                  "createdAt": "2026-11-10T00:00:00Z",
                  "featureHtml": "<p>pre-006</p>",
                  "playerCount": 2,
                  "players": [{"name":"Joueur-1","archetype":"HAPPY"}],
                  "turnOrder": ["Joueur-1"],
                  "deck": [{"id":"VE2","value":"2","setId":"webapp","suitId":"VE","suitName":"VE","language":"fr","description":"d"}]
                }
                """);
        Files.writeString(tempDir.resolve(id).resolve("leg-001-joueur-1.json"), """
                {
                  "cardName": "VE2",
                  "scenario": "s",
                  "explanation": "e",
                  "entries": [],
                  "validation": "VALIDATED"
                }
                """);

        var loaded = store.loadAtelier(id);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().tours()).isEmpty();
        assertThat(loaded.get().scores()).isEmpty();
        assertThat(loaded.get().winner()).isEmpty();

        List<Leg> legs = store.loadLegs(id);
        assertThat(legs).hasSize(1);
        assertThat(legs.get(0).turnIndex()).isZero(); // primitive default on legacy read
        assertThat(legs.get(0).points()).isZero();
        assertThat(legs.get(0).majorSuit()).isNull();
        assertThat(legs.get(0).validation()).isEqualTo(ValidationStatus.VALIDATED);
    }
}
