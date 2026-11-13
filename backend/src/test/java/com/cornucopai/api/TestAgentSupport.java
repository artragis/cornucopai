package com.cornucopai.api;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import java.util.UUID;

/** Spec 009 test helper: creates configurable agents over HTTP and returns their ids. */
public final class TestAgentSupport {

  private TestAgentSupport() {}

  public static String createAgentId() {
    return createAgentId("Agent-" + UUID.randomUUID().toString().substring(0, 8));
  }

  public static String createAgentId(String name) {
    return given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"agentName": "%s", "openAiCompatibleUrl": "http://localhost:1235/v1",
             "apiKeyVar": "", "modelName": "test-model", "personalityPrompt": "Personnalité test."}
            """
                .formatted(name))
        .when()
        .post("/api/agents")
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }

  public static String createAgentIdWithKeyVar(String name, String apiKeyVar) {
    return given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"agentName": "%s", "openAiCompatibleUrl": "https://api.mistral.ai/v1",
             "apiKeyVar": "%s", "modelName": "mistral-large-latest", "personalityPrompt": "Personnalité test."}
            """
                .formatted(name, apiKeyVar))
        .when()
        .post("/api/agents")
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }

  /** JSON array of {@code n} freshly created agent ids, e.g. ["id1","id2"]. */
  public static String agentIdsJson(int n) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < n; i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append("\"").append(createAgentId()).append("\"");
    }
    return sb.append("]").toString();
  }
}
