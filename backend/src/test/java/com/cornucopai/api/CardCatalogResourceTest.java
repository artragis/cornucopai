package com.cornucopai.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CardCatalogResourceTest {

  @Test
  void getCardCatalogReturnsSetsWithFamiliesAndLanguages() {
    given()
        .when()
        .get("/api/card-catalog")
        .then()
        .statusCode(200)
        // availableLanguages = union, lowercase, deduped
        .body("availableLanguages", containsInAnyOrder("en", "fr"))
        // 2 sets in the fixture, latest version only (webapp 3.0, not 2.2)
        .body("sets.size()", is(2))
        .body("sets.id", containsInAnyOrder("webapp", "eop"))
        .body("sets.version", containsInAnyOrder("3.0", "5.0"))
        // webapp has en+fr, eop only en
        .body("sets.find { it.id == 'webapp' }.languages", containsInAnyOrder("en", "fr"))
        .body("sets.find { it.id == 'eop' }.languages", contains("en"))
        // families of webapp parsed from the en file
        .body("sets.find { it.id == 'webapp' }.families.id", containsInAnyOrder("VE", "AT", "WC"))
        .body("sets.find { it.id == 'webapp' }.totalCards", is(5));
  }
}
