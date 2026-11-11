package com.cornucopai.game.model;

import com.cornucopai.game.AtelierStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T002: v2 leg schema — structured entries round-trip, legacy v1 `precisions` read
 * conversion, unknown `kind` tolerated (FR-001, FR-003, R-035/R-036).
 */
class LegEntryTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private AtelierStore store() {
        return new AtelierStore(tempDir, mapper);
    }

    @Test
    void freshLegHasEmptyEntriesAndPendingValidation() {
        Leg leg = new Leg("VE2", "scénario", "explication");
        assertThat(leg.entries()).isEmpty();
        assertThat(leg.validation()).isEqualTo(ValidationStatus.PENDING);
    }

    @Test
    void entriesRoundTripThroughTheStoreAsStructuredObjects() throws Exception {
        AtelierStore store = store();
        String id = store.createAtelierDir(store.newAtelierId());
        Leg leg = new Leg("VE2", "scénario", "explication");
        leg.addEntry(LegEntry.precision("Joueur-1", "ma précision"));
        leg.addEntry(LegEntry.question("Joueur-2", "Que se passe-t-il si ça échoue ?", true, "Le flow est interrompu"));
        leg.addEntry(LegEntry.question("Joueur-3", "As-tu vérifié dans le code ?", false, null));
        store.saveLeg(id, 1, "joueur-2", leg);

        JsonNode json = mapper.readTree(tempDir.resolve(id).resolve("leg-001-joueur-2.json").toFile());
        assertThat(json.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "cardName", "scenario", "explanation", "entries", "validation",
                "turnIndex", "majorSuit", "suitId", "cardValue", "points", // spec 006 v3
                "votes"); // spec 007 v4 (R-053)
        JsonNode entries = json.get("entries");
        assertThat(entries).hasSize(3);
        assertThat(entries.get(0).get("kind").asText()).isEqualTo("PRECISION");
        assertThat(entries.get(0).get("author").asText()).isEqualTo("Joueur-1");
        assertThat(entries.get(0).has("knows")).isFalse();
        assertThat(entries.get(1).get("kind").asText()).isEqualTo("QUESTION");
        assertThat(entries.get(1).get("knows").asBoolean()).isTrue();
        assertThat(entries.get(1).get("answer").asText()).isEqualTo("Le flow est interrompu");
        assertThat(entries.get(2).get("knows").asBoolean()).isFalse();
        assertThat(entries.get(2).has("answer")).isFalse();

        Leg loaded = store.loadLegs(id).get(0);
        assertThat(loaded.entries()).hasSize(3);
        assertThat(loaded.entries().get(1).isQuestion()).isTrue();
        assertThat(loaded.entries().get(1).knows()).isTrue();
        assertThat(loaded.entries().get(1).answer()).isEqualTo("Le flow est interrompu");
    }

    @Test
    void legacyV1PrecisionsAreConvertedOnRead() throws Exception {
        AtelierStore store = store();
        String id = store.createAtelierDir(store.newAtelierId());
        Path legFile = tempDir.resolve(id).resolve("leg-001-joueur-1.json");
        Files.writeString(legFile, """
                {
                  "cardName" : "VE2",
                  "scenario" : "scénario v1",
                  "explanation" : "explication v1",
                  "precisions" : ["une précision", "réponse à une question"],
                  "validation" : "VALIDATED"
                }
                """);

        List<Leg> legs = store.loadLegs(id);
        assertThat(legs).hasSize(1);
        Leg leg = legs.get(0);
        assertThat(leg.entries()).hasSize(2);
        assertThat(leg.entries()).allSatisfy(e -> {
            assertThat(e.kind()).isEqualTo(LegEntry.Kind.PRECISION);
            assertThat(e.author()).isEqualTo("inconnu");
        });
        assertThat(leg.entries().get(0).text()).isEqualTo("une précision");
        assertThat(leg.validation()).isEqualTo(ValidationStatus.VALIDATED);

        // and re-writing it produces v2 (entries, no precisions)
        store.saveLeg(id, 1, "joueur-1", leg);
        JsonNode rewritten = mapper.readTree(legFile.toFile());
        assertThat(rewritten.has("precisions")).isFalse();
        assertThat(rewritten.get("entries")).hasSize(2);
    }

    @Test
    void unknownKindIsToleratedOnRead() throws Exception {
        AtelierStore store = store();
        String id = store.createAtelierDir(store.newAtelierId());
        Files.writeString(tempDir.resolve(id).resolve("leg-002-joueur-1.json"), """
                {
                  "cardName" : "VE3",
                  "scenario" : "s",
                  "explanation" : "e",
                  "entries" : [ {"kind": "FUTURE_KIND", "author": "Joueur-1", "text": "quelque chose"} ],
                  "validation" : "PENDING"
                }
                """);
        List<Leg> legs = store.loadLegs(id);
        assertThat(legs).hasSize(1);
        assertThat(legs.get(0).entries().get(0).kind()).isEqualTo("FUTURE_KIND");
    }

    @Test
    void factoriesSetFieldsAndValidation() {
        LegEntry p = LegEntry.precision("Joueur-1", "texte");
        assertThat(p.kind()).isEqualTo(LegEntry.Kind.PRECISION);
        assertThat(p.knows()).isNull();
        assertThat(p.answer()).isNull();
        assertThat(p.isQuestion()).isFalse();

        LegEntry q = LegEntry.question("Joueur-2", "question ?", false, null);
        assertThat(q.isQuestion()).isTrue();
        assertThat(q.knows()).isFalse();
        assertThat(q.answer()).isNull();
    }
}
