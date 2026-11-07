package com.cornucopai.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Contract test for GET /api/health — see
 * specs/001-project-bootstrap/contracts/health-api.yaml
 * Expects 200 with body {"status":"UP"}.
 */
@QuarkusTest
class HealthResourceTest {

    @Test
    void healthEndpointReturnsUp() {
        given()
            .when().get("/api/health")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }
}
