package com.cornucopai.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * T009 (spec 009, US1): CRUD contract of {@code contracts/agents-api.yaml} v1.0.0 — POST 201 +
 * Location + server-generated uuid (FR-003), 400 on each FR-019 violation, 409 on duplicate
 * agentName (FR-018), GET list / detail 404, PUT keeps id (FR-004), DELETE 204 then 404 (FR-005).
 * The API key VALUE never crosses the API — only the apiKeyVar name (FR-006). Each test uses a
 * unique agent name because the Quarkus test app shares one agents dir.
 */
@QuarkusTest
class AgentResourceTest {

  private static String bodyFor(String name) {
    String body =
        "{\"agentName\": \"%s\", \"openAiCompatibleUrl\": \"http://localhost:1235/v1\", "
            + "\"apiKeyVar\": \"\", \"modelName\": \"qwen3-coder\","
            + " \"personalityPrompt\": \"Tu es tatillon.\"}";
    return body.formatted(name);
  }

  private static String uniqueName() {
    return "Agent-" + UUID.randomUUID().toString().substring(0, 8);
  }

  private String createAgent(String body) {
    return given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/api/agents")
        .then()
        .statusCode(201)
        .header("Location", containsString("/api/agents/"))
        .extract()
        .path("id");
  }

  @Test
  void postCreatesAgentWithGeneratedUuidAndLocationHeader() {
    String name = uniqueName();
    String id = createAgent(bodyFor(name));
    given()
        .when()
        .get("/api/agents/" + id)
        .then()
        .statusCode(200)
        .body("id", is(id))
        .body("agentName", is(name))
        .body("openAiCompatibleUrl", is("http://localhost:1235/v1"))
        .body("apiKeyVar", is(""))
        .body("modelName", is("qwen3-coder"))
        .body("personalityPrompt", is("Tu es tatillon."));
  }

  @Test
  void postRejectsEachFr019ViolationWith400() {
    String name = uniqueName();
    String[][] badBodies = {
      {"blank agentName", bodyFor(name).replace("\"" + name + "\"", "\" \"")},
      {"non-http url", bodyFor(name).replace("http://localhost:1235/v1", "ftp://x")},
      {"relative url", bodyFor(name).replace("http://localhost:1235/v1", "localhost:1235/v1")},
      {
        "invalid apiKeyVar", bodyFor(name).replace("\"apiKeyVar\": \"\"", "\"apiKeyVar\": \"9BAD\"")
      },
      {"blank modelName", bodyFor(name).replace("\"qwen3-coder\"", "\" \"")},
      {"blank personalityPrompt", bodyFor(name).replace("\"Tu es tatillon.\"", "\"\"")},
    };
    for (String[] bad : badBodies) {
      given()
          .contentType(ContentType.JSON)
          .body(bad[1])
          .when()
          .post("/api/agents")
          .then()
          .statusCode(400)
          .body("error", containsString(""));
    }
  }

  @Test
  void postWithDuplicateAgentNameReturns409() {
    String name = uniqueName();
    createAgent(bodyFor(name));
    given()
        .contentType(ContentType.JSON)
        .body(bodyFor(name))
        .when()
        .post("/api/agents")
        .then()
        .statusCode(409)
        .body("error", containsString(name));
  }

  @Test
  void listReturnsAgents() {
    String n1 = uniqueName();
    String n2 = uniqueName();
    createAgent(bodyFor(n1));
    createAgent(bodyFor(n2));
    given()
        .when()
        .get("/api/agents")
        .then()
        .statusCode(200)
        .body("agentName", hasItem(n1))
        .body("agentName", hasItem(n2));
  }

  @Test
  void getUnknownIdReturns404() {
    given()
        .when()
        .get("/api/agents/00000000-0000-0000-0000-000000000000")
        .then()
        .statusCode(404)
        .body("error", containsString("unknown agent id"));
  }

  @Test
  void putUpdatesFieldsButKeepsTheId() {
    String name = uniqueName();
    String id = createAgent(bodyFor(name));
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"agentName": "%s", "openAiCompatibleUrl": "https://api.mistral.ai/v1",
             "apiKeyVar": "MISTRAL_API_KEY", "modelName": "mistral-large-latest",
             "personalityPrompt": "Nouveau prompt."}
            """
                .formatted(uniqueName()))
        .when()
        .put("/api/agents/" + id)
        .then()
        .statusCode(200)
        .body("id", is(id))
        .body("openAiCompatibleUrl", is("https://api.mistral.ai/v1"))
        .body("apiKeyVar", is("MISTRAL_API_KEY"));
    // the key value is never part of the payload — only the variable name
    given()
        .when()
        .get("/api/agents/" + id)
        .then()
        .body("apiKeyVar", is("MISTRAL_API_KEY"))
        .body("$", not(hasItem("apiKey")));
  }

  @Test
  void putWithUnknownIdReturns404AndDuplicateNameReturns409() {
    String id = createAgent(bodyFor(uniqueName()));
    String otherName = uniqueName();
    createAgent(bodyFor(otherName));
    given()
        .contentType(ContentType.JSON)
        .body(bodyFor(otherName))
        .when()
        .put("/api/agents/" + id)
        .then()
        .statusCode(409);
    given()
        .contentType(ContentType.JSON)
        .body(bodyFor(uniqueName()))
        .when()
        .put("/api/agents/00000000-0000-0000-0000-000000000000")
        .then()
        .statusCode(404);
  }

  @Test
  void deleteReturns204ThenGetReturns404() {
    String id = createAgent(bodyFor(uniqueName()));
    given().when().delete("/api/agents/" + id).then().statusCode(204);
    given().when().get("/api/agents/" + id).then().statusCode(404);
    given().when().delete("/api/agents/" + id).then().statusCode(404);
  }
}
