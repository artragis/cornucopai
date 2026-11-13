package com.cornucopai.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * T022 (spec 009, US2): POST /api/ateliers with {@code agentIds[]} (API v0.8.0) — 201 with players
 * carrying agentId + Joueur-N names (FR-016), duplicate agent id allowed (FR-009), unknown id →
 * 400, fewer than 2 ids → 400 (FR-010), missing apiKeyVar env var → 400 naming it (FR-013, SC-009).
 * One single family (webapp:VE) per game (spec 004 FR-014); CI runs on the fake brain.
 */
@QuarkusTest
class AtelierResourceAgentsTest {

  private static final Path TEST_ROOT = Path.of("target/test-ateliers");

  private static String body(String agentIdsArray) {
    return "{\"agentIds\": "
        + agentIdsArray
        + ", \"featureHtml\": \"<p>f</p>\", \"language\": \"fr\", \"families\": [\"webapp:VE\"]}";
  }

  @Test
  void postWithAgentIdsRunsTheGameAndPersistsSnapshots() throws Exception {
    String a = TestAgentSupport.createAgentId();
    String b = TestAgentSupport.createAgentId();
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(body("[\"" + a + "\", \"" + b + "\"]"))
            .when()
            .post("/api/ateliers")
            .then()
            .statusCode(201)
            .body("playerCount", is(2))
            .body("players.size()", is(2))
            .body("players.agentId", org.hamcrest.Matchers.containsInAnyOrder(a, b))
            .body("players.name", everyItem(org.hamcrest.Matchers.matchesRegex("Joueur-[12]")))
            .body("agentSnapshots.size()", is(2))
            .body("agentSnapshots.id", everyItem(notNullValue()))
            .extract()
            .path("id");

    JsonNode json =
        new ObjectMapper().readTree(TEST_ROOT.resolve(id).resolve("atelier.json").toFile());
    assertThat(json.get("agentSnapshots")).hasSize(2);
    assertThat(json.get("agentSnapshots").get(0).get("apiKeyVar").asText()).isEmpty();
    // FR-017: no API key value is ever persisted
    assertThat(json.get("agentSnapshots").toString()).doesNotContain("apiKey\"");
    // players are index-aligned with snapshots
    assertThat(json.get("players").get(0).get("agentId").asText())
        .isEqualTo(json.get("agentSnapshots").get(0).get("id").asText());
  }

  @Test
  void sameAgentTwiceIsAllowedTwoParticipantsShareIt() {
    String a = TestAgentSupport.createAgentId();
    given()
        .contentType(ContentType.JSON)
        .body(body("[\"" + a + "\", \"" + a + "\"]"))
        .when()
        .post("/api/ateliers")
        .then()
        .statusCode(201)
        .body("players.size()", is(2))
        .body("players.agentId", everyItem(is(a)))
        .body("players.name", hasSize(2));
  }

  @Test
  void unknownAgentIdIs400() {
    String a = TestAgentSupport.createAgentId();
    given()
        .contentType(ContentType.JSON)
        .body(body("[\"" + a + "\", \"00000000-0000-0000-0000-000000000000\"]"))
        .when()
        .post("/api/ateliers")
        .then()
        .statusCode(400)
        .body("error", containsString("unknown agent id"));
  }

  @Test
  void fewerThanTwoAgentIdsIs400() {
    String a = TestAgentSupport.createAgentId();
    given()
        .contentType(ContentType.JSON)
        .body(body("[\"" + a + "\"]"))
        .when()
        .post("/api/ateliers")
        .then()
        .statusCode(400)
        .body("error", containsString("agentIds"));
  }

  @Test
  void missingApiKeyVarEnvVarIs400NamingTheVariable() {
    String missing =
        "CORNUCOPAI_TEST_MISSING_" + UUID.randomUUID().toString().replace("-", "_").toUpperCase();
    String a =
        TestAgentSupport.createAgentIdWithKeyVar(
            "KeyAgent-" + UUID.randomUUID().toString().substring(0, 6), missing);
    String b = TestAgentSupport.createAgentId();
    given()
        .contentType(ContentType.JSON)
        .body(body("[\"" + a + "\", \"" + b + "\"]"))
        .when()
        .post("/api/ateliers")
        .then()
        .statusCode(400)
        .body("error", containsString(missing));
  }
}
