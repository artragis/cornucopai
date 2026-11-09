package com.cornucopai.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * US4 (spec 003): when the Cornucopia repo is not configured (CORNUCOPIA_HOME
 * blank), the catalog is empty and launching a game is refused with a 400 (FR-011,
 * FR-012). This test runs under a profile that blanks cornucopia-home.
 */
@QuarkusTest
@io.quarkus.test.junit.TestProfile(CardErrorCasesTest.NoRepoProfile.class)
class CardErrorCasesTest {

    public static class NoRepoProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            // invalid/missing repo -> empty catalog (FR-011); avoids the empty-string config pitfall
            return Map.of("cornucopai.cornucopia-home", "/nonexistent/cornucopia");
        }
    }

    @Test
    void catalogIsEmptyWhenRepoNotConfigured() {
        given().when().get("/api/card-catalog")
                .then().statusCode(200)
                .body("sets", hasSize(0))
                .body("availableLanguages", hasSize(0));
    }

    @Test
    void launchIsRefused400WhenRepoNotConfigured() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"playerCount\": 2, \"featureHtml\": \"<p>x</p>\", \"families\": [\"webapp:VE\"]}")
                .when().post("/api/ateliers")
                .then().statusCode(400)
                .body("error", containsString("unknown family"));
    }
}
