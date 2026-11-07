package com.cornucopai.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AtelierResourceTest {

    private static final Path TEST_ROOT = Path.of("target/test-ateliers");
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void cleanTestAteliers() throws IOException {
        if (Files.exists(TEST_ROOT)) {
            try (Stream<Path> walk = Files.walk(TEST_ROOT)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    @Test
    void postCreatesRunsAndPersistsAFullGame() throws IOException {
        String id = given()
                .contentType(ContentType.JSON)
                .body("{\"playerCount\": 4, \"featureHtml\": \"<p>Feature login OAuth2</p>\"}")
                .when().post("/api/ateliers")
                .then().statusCode(201)
                .body("id", matchesRegex("atelier\\d{8}-\\d{6}(-\\d+)?"))
                .body("playerCount", is(4))
                .body("featureHtml", is("<p>Feature login OAuth2</p>"))
                .body("players.size()", is(4))
                .body("players.name", contains("Joueur-1", "Joueur-2", "Joueur-3", "Joueur-4"))
                .body("players.archetype", contains("GRUMPY", "HAPPY", "GREEDY", "PROF"))
                .body("turnOrder.size()", is(4))
                .body("legs.size()", is(8))
                .body("legs.cardName", everyItem(startsWith("Carte-Placeholder-")))
                .body("legs.validation", everyItem(anyOf(is("VALIDATED"), is("REJECTED"))))
                .body("legs.validation", not(hasItem("PENDING")))
                .body("finishedAt", notNullValue())
                .extract().path("id");

        // with 4 players (Grumpy/Happy/Greedy/Prof), Grumpy's 2 legs are REJECTED, the 6 others VALIDATED
        List<String> validations = given()
                .when().get("/api/ateliers/" + id)
                .then().statusCode(200)
                .extract().jsonPath().getList("legs.validation", String.class);
        assertThat(validations).containsExactlyInAnyOrder(
                "VALIDATED", "VALIDATED", "VALIDATED", "VALIDATED", "VALIDATED", "VALIDATED",
                "REJECTED", "REJECTED");

        // persistence on disk: atelier.json + 8 leg files with exactly the 5 contract fields
        Path dir = TEST_ROOT.resolve(id);
        assertThat(dir.resolve("atelier.json")).exists();
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> legFiles = files.filter(p -> p.getFileName().toString().startsWith("leg-")).toList();
            assertThat(legFiles).hasSize(8);
            for (Path legFile : legFiles) {
                JsonNode json = mapper.readTree(legFile.toFile());
                assertThat(json.fieldNames()).toIterable().containsExactlyInAnyOrder(
                        "cardName", "scenario", "explanation", "precisions", "validation");
            }
        }
    }

    @Test
    void postWithLessThanTwoPlayersIs400() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"playerCount\": 1, \"featureHtml\": \"<p>x</p>\"}")
                .when().post("/api/ateliers")
                .then().statusCode(400)
                .body("error", notNullValue());
    }

    @Test
    void postWithEmptyFeatureIs400() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"playerCount\": 4, \"featureHtml\": \"\"}")
                .when().post("/api/ateliers")
                .then().statusCode(400)
                .body("error", notNullValue());
    }

    @Test
    void getListsAndReturnsDetailAnd404OnUnknown() {
        String id = given()
                .contentType(ContentType.JSON)
                .body("{\"playerCount\": 2, \"featureHtml\": \"<p>Feature X</p>\"}")
                .when().post("/api/ateliers")
                .then().statusCode(201)
                .extract().path("id");

        given().when().get("/api/ateliers")
                .then().statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("id", hasItem(id));

        given().when().get("/api/ateliers/" + id)
                .then().statusCode(200)
                .body("id", is(id))
                .body("legs.size()", is(8));

        given().when().get("/api/ateliers/atelier2099-01-01-000000")
                .then().statusCode(404)
                .body("error", notNullValue());
    }
}
