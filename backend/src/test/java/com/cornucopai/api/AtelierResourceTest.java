package com.cornucopai.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AtelierResourceTest {

  private static final Path TEST_ROOT = Path.of("target/test-ateliers");
  private final ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void cleanTestAteliers() throws IOException {
    if (Files.exists(TEST_ROOT)) {
      try (Stream<Path> walk = Files.walk(TEST_ROOT)) {
        walk.sorted(Comparator.reverseOrder())
            .forEach(p -> {
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
  void postCreatesRunsAndPersistsFullGame() throws IOException {
    String id =
        given()
            .contentType(ContentType.JSON)
            // spec 004 (FR-014): ONE single family per test game -> bounded turns (webapp:VE = 2
            // cards -> 2 legs)
            .body(
                "{\"agentIds\": "
                    + TestAgentSupport.agentIdsJson(4)
                    + ", \"featureHtml\": \"<p>Feature login OAuth2</p>\", "
                    + "\"language\": \"fr\", \"families\": [\"webapp:VE\"]}")
            .when()
            .post("/api/ateliers")
            .then()
            .statusCode(201)
            .body("id", matchesRegex("atelier\\d{8}-\\d{6}(-\\d+)?"))
            .body("playerCount", is(4))
            .body("featureHtml", is("<p>Feature login OAuth2</p>"))
            .body("players.size()", is(4))
            .body("players.name", contains("Joueur-1", "Joueur-2", "Joueur-3", "Joueur-4"))
            .body("players.agentId", everyItem(notNullValue()))
            .body("turnOrder.size()", is(4))
            // spec 003 + spec 004 (FR-014): real cards from the single family webapp:VE (2) -> 2
            // cards -> 2 legs
            .body("language", is("fr"))
            .body("selectedFamilies", containsInAnyOrder("webapp:VE"))
            .body("sets.size()", is(1))
            .body("sets[0].id", is("webapp"))
            .body("sets[0].resolvedLanguage", is("fr"))
            .body("deck.size()", is(2))
            .body("deck.id", containsInAnyOrder("VE2", "VE3"))
            .body("deck.language", everyItem(is("fr")))
            .body("deck.description", everyItem(notNullValue()))
            .body("legs.size()", is(2))
            .body("legs.cardName", everyItem(isIn(List.of("VE2", "VE3"))))
            .body("legs.validation", everyItem(anyOf(is("VALIDATED"), is("REJECTED"))))
            .body("legs.validation", not(hasItem("PENDING")))
            .body("finishedAt", notNullValue())
            // spec 006 v0.6.0: tours, scores, winner, and v3 leg fields (T017)
            .body("tours.size()", is(1))
            .body("tours[0].majorSuit", is("VE"))
            .body("tours[0].leader", notNullValue())
            .body("scores", notNullValue())
            .body("winner", notNullValue())
            .body("legs.turnIndex", everyItem(is(1)))
            .body("legs.suitId", everyItem(is("VE")))
            .body("legs.points", everyItem(anyOf(is(0), is(1), is(2))))
            .extract()
            .path("id");

    // every leg's cardName references a real card present in the persisted deck
    List<String> deckIds =
        given()
            .when()
            .get("/api/ateliers/" + id)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("deck.id", String.class);
    List<String> legCardNames =
        given()
            .when()
            .get("/api/ateliers/" + id)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("legs.cardName", String.class);
    assertThat(legCardNames).allMatch(deckIds::contains);

    // persistence on disk: atelier.json (deck = full Card objects, + agentConfigs from spec 004) +
    // 2 leg files with exactly the 5 contract fields
    Path dir = TEST_ROOT.resolve(id);
    assertThat(dir.resolve("atelier.json")).exists();
    JsonNode atelierJson = mapper.readTree(dir.resolve("atelier.json").toFile());
    assertThat(atelierJson.get("deck").isArray()).isTrue();
    assertThat(atelierJson.get("deck").get(0).has("id")).isTrue();
    assertThat(atelierJson.get("deck").get(0).has("description")).isTrue();
    assertThat(atelierJson.get("language").asText()).isEqualTo("fr");
    try (Stream<Path> files = Files.list(dir)) {
      List<Path> legFiles =
          files.filter(p -> p.getFileName().toString().startsWith("leg-")).toList();
      assertThat(legFiles).hasSize(2);
      for (Path legFile : legFiles) {
        JsonNode json = mapper.readTree(legFile.toFile());
        assertThat(json.fieldNames())
            .toIterable()
            .containsExactlyInAnyOrder(
                "cardName",
                "scenario",
                "explanation",
                "entries",
                "validation",
                "turnIndex",
                "majorSuit",
                "suitId",
                "cardValue",
                "points", // spec 006 v3
                "votes"); // spec 007 v4 (R-053)
      }
    }
  }

  @Test
  void spec007InheritFactsFromCopiesProjectFacts() throws IOException {
    // create a first atelier, then inject a project fact directly into its persisted JSON
    String firstId =
        given()
            .contentType(ContentType.JSON)
            .body(
                "{\"agentIds\": "
                    + TestAgentSupport.agentIdsJson(2)
                    + ", \"featureHtml\": \"<p>f1</p>\", \"language\": \"fr\", "
                    + "\"families\": [\"webapp:VE\"]}")
            .when()
            .post("/api/ateliers")
            .then()
            .statusCode(201)
            .body("projectFacts", notNullValue())
            .extract()
            .path("id");
    Path firstJson = TEST_ROOT.resolve(firstId).resolve("atelier.json");
    JsonNode node = mapper.readTree(firstJson.toFile());
    ((com.fasterxml.jackson.databind.node.ObjectNode) node)
        .set(
            "projectFacts",
            mapper.readTree(
                "[{\"text\":\"pas d'API REST\",\"author\":\"Joueur-1\",\"turnIndex\":1,"
                    + "\"timestamp\":\"2026-11-11T10:00:00Z\"}]"));
    Files.writeString(firstJson, mapper.writeValueAsString(node));

    // GET list must flag it hasProjectFacts=true (API v0.7.0, R-056)
    given()
        .when()
        .get("/api/ateliers")
        .then()
        .statusCode(200)
        .body("find { it.id == '" + firstId + "' }.hasProjectFacts", is(true));

    // create a second atelier inheriting the facts (FR-007)
    given()
        .contentType(ContentType.JSON)
        .body(
            "{\"agentIds\": "
                + TestAgentSupport.agentIdsJson(2)
                + ", \"featureHtml\": \"<p>f2</p>\", \"language\": \"fr\", "
                + "\"families\": [\"webapp:VE\"], \"inheritFactsFrom\": \""
                + firstId
                + "\"}")
        .when()
        .post("/api/ateliers")
        .then()
        .statusCode(201)
        .body("projectFacts.size()", is(1))
        .body("projectFacts[0].text", is("pas d'API REST"));
  }

  @Test
  void spec007UnknownInheritFactsFromIs400() {
    given()
        .contentType(ContentType.JSON)
        .body(
            "{\"agentIds\": "
                + TestAgentSupport.agentIdsJson(2)
                + ", \"featureHtml\": \"<p>x</p>\", \"families\": [\"webapp:VE\"], "
                + "\"inheritFactsFrom\": \"atelier-does-not-exist\"}")
        .when()
        .post("/api/ateliers")
        .then()
        .statusCode(400);
  }

  @Test
  void postWithLessThanTwoPlayersIs400() {
    given()
        .contentType(ContentType.JSON)
        .body(
            "{\"agentIds\": "
                + TestAgentSupport.agentIdsJson(1)
                + ", \"featureHtml\": \"<p>x</p>\", \"families\": [\"eop:SP\"]}")
        .when()
        .post("/api/ateliers")
        .then()
        .statusCode(400)
        .body("error", notNullValue());
  }

  @Test
  void postWithEmptyFeatureIs400() {
    given()
        .contentType(ContentType.JSON)
        .body(
            "{\"agentIds\": "
                + TestAgentSupport.agentIdsJson(4)
                + ", \"featureHtml\": \"\", \"families\": [\"eop:SP\"]}")
        .when()
        .post("/api/ateliers")
        .then()
        .statusCode(400)
        .body("error", notNullValue());
  }

  @Test
  void getListsAndReturnsDetailAnd404OnUnknown() {
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(
                "{\"agentIds\": "
                    + TestAgentSupport.agentIdsJson(2)
                    + ", \"featureHtml\": \"<p>Feature X</p>\", "
                    + "\"families\": [\"eop:SP\"]}")
            .when()
            .post("/api/ateliers")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given()
        .when()
        .get("/api/ateliers")
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1))
        .body("id", hasItem(id));

    given()
        .when()
        .get("/api/ateliers/" + id)
        .then()
        .statusCode(200)
        .body("id", is(id))
        .body("legs.size()", is(2)); // eop:SP -> 2 cards

    given()
        .when()
        .get("/api/ateliers/atelier2099-01-01-000000")
        .then()
        .statusCode(404)
        .body("error", notNullValue());
  }
}
