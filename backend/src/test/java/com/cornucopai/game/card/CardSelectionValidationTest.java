package com.cornucopai.game.card;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

/** US2 validation: empty families -> 400, unknown family -> 400 (FR-006, FR-012). */
@QuarkusTest
class CardSelectionValidationTest {

    @Test
    void emptyFamiliesIs400() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"playerCount\": 2, \"featureHtml\": \"<p>x</p>\", \"language\": \"fr\", \"families\": []}")
                .when().post("/api/ateliers")
                .then().statusCode(400)
                .body("error", containsString("families"));
    }

    @Test
    void missingFamiliesIs400() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"playerCount\": 2, \"featureHtml\": \"<p>x</p>\"}")
                .when().post("/api/ateliers")
                .then().statusCode(400)
                .body("error", notNullValue());
    }

    @Test
    void unknownFamilyIs400AndNamesIt() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"playerCount\": 2, \"featureHtml\": \"<p>x</p>\", \"families\": [\"eop:ZZ\"]}")
                .when().post("/api/ateliers")
                .then().statusCode(400)
                .body("error", containsString("eop:ZZ"));
    }

    @Test
    void knownFamilyIsAccepted201() {
        // fixture has eop:SP; deck is still placeholder in this phase, but the request is valid
        given()
                .contentType(ContentType.JSON)
                .body("{\"playerCount\": 2, \"featureHtml\": \"<p>x</p>\", \"language\": \"fr\", \"families\": [\"eop:SP\"]}")
                .when().post("/api/ateliers")
                .then().statusCode(201);
    }
}
