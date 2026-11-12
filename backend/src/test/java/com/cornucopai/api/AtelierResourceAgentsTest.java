package com.cornucopai.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * T014 (US1): per-agent LLM provider config on POST /api/ateliers — validation (400), defaults
 * (FR-002) and persistence of agentConfigs (FR-012). One single family (webapp:VE) per game
 * (FR-014); CI runs on the fake brain (cornucopai.llm.brain=fake).
 */
@QuarkusTest
class AtelierResourceAgentsTest {

  private static final Path TEST_ROOT = Path.of("target/test-ateliers");

  @Test
  void postWithoutAgentsUsesLocalDefaultConfigAndPersistsIt() throws Exception {
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(
                "{\"playerCount\": 2, \"featureHtml\": \"<p>f</p>\", \"language\": \"fr\", "
                    + "\"families\": [\"webapp:VE\"]}")
            .when()
            .post("/api/ateliers")
            .then()
            .statusCode(201)
            .body("agentConfigs.size()", is(2))
            .body("agentConfigs[0].providerType", is("OPENAI_COMPATIBLE"))
            .body("agentConfigs[0].baseUrl", is("http://localhost:1235/v1"))
            .extract()
            .path("id");

    JsonNode json =
        new ObjectMapper().readTree(TEST_ROOT.resolve(id).resolve("atelier.json").toFile());
    assertThat(json.get("agentConfigs")).hasSize(2);
    assertThat(json.get("agentConfigs").get(0).get("baseUrl").asText())
        .isEqualTo("http://localhost:1235/v1");
  }

  @Test
  void postWithPerAgentConfigsReturnsThemIndexAlignedWithPlayers() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"playerCount": 2, "featureHtml": "<p>f</p>", "families": ["webapp:VE"],
             "agents": [
               {"name": "Alice", "archetype": "HAPPY", "providerType": "OPENAI_COMPATIBLE", "baseUrl": "http://localhost:1235/v1"},
               {"name": "Bob", "archetype": "GRUMPY", "providerType": "ACP", "acpProvider": "mistral-vibe"}
             ]}
            """)
        .when()
        .post("/api/ateliers")
        .then()
        .statusCode(201)
        .body("players.name", is(List.of("Alice", "Bob")))
        .body("agentConfigs[0].providerType", is("OPENAI_COMPATIBLE"))
        .body("agentConfigs[1].providerType", is("ACP"))
        .body("agentConfigs[1].acpProvider", is("mistral-vibe"));
  }

  @Test
  void invalidAgentConfigsAre400() {
    String base =
        "{\"playerCount\": 2, \"featureHtml\": \"<p>f</p>\", \"families\": [\"webapp:VE\"], "
            + "\"agents\": [%s]}";
    given()
        .contentType(ContentType.JSON)
        .body(base.formatted("{\"providerType\": \"FOO\", \"baseUrl\": \"http://x\"}"))
        .when()
        .post("/api/ateliers")
        .then()
        .statusCode(400)
        .body("error", containsString("unknown providerType"));
    given()
        .contentType(ContentType.JSON)
        .body(base.formatted("{\"providerType\": \"ACP\"}"))
        .when()
        .post("/api/ateliers")
        .then()
        .statusCode(400)
        .body("error", containsString("acpProvider"));
    given()
        .contentType(ContentType.JSON)
        .body(base.formatted("{\"providerType\": \"OPENAI_COMPATIBLE\", \"baseUrl\": \" \"}"))
        .when()
        .post("/api/ateliers")
        .then()
        .statusCode(400)
        .body("error", containsString("baseUrl"));
    given()
        .contentType(ContentType.JSON)
        .body(
            "{\"playerCount\": 2, \"featureHtml\": \"<p>f</p>\", "
                + "\"families\": [\"webapp:VE\"], \"agents\": ["
                + "{\"providerType\": \"ACP\", \"acpProvider\": \"github-copilot\"}, "
                + "{\"providerType\": \"ACP\", \"acpProvider\": \"github-copilot\"}, "
                + "{\"providerType\": \"ACP\", \"acpProvider\": \"mistral-vibe\"}]}")
        .when()
        .post("/api/ateliers")
        .then()
        .statusCode(400)
        .body("error", containsString("playerCount"));
  }
}
