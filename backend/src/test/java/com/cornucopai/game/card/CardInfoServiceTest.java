package com.cornucopai.game.card;

import static org.assertj.core.api.Assertions.assertThat;

import com.cornucopai.game.card.model.CardInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * T029 (US3): the card-info service aggregates the three sources — cards YAML, mappings YAML, and
 * the rich content under cornucopia.owasp.org/data/cards (R-024, FR-006..FR-009). Runs against the
 * hermetic fixture.
 */
class CardInfoServiceTest {

  private static final Path FIXTURE = Path.of("src/test/resources/cornucopia-fixture");

  private final CardInfoService service = new CardInfoService(FIXTURE);

  @Test
  void ve2AggregatesYamlMappingsAndRichContent() {
    Optional<CardInfo> info = service.infoFor("VE2");
    assertThat(info).isPresent();
    CardInfo c = info.get();
    assertThat(c.id()).isEqualTo("VE2");
    assertThat(c.value()).isEqualTo("2");
    assertThat(c.setId()).isEqualTo("webapp");
    assertThat(c.version()).isEqualTo("3.0");
    assertThat(c.suitId()).isEqualTo("VE");
    assertThat(c.suitName()).isEqualTo("DATA VALIDATION & ENCODING");
    assertThat(c.description()).contains("Robert");
    assertThat(c.url()).isEqualTo("https://cornucopia.owasp.org/cards/VE2");
    // mappings from webapp-mappings-3.0.yaml
    assertThat(c.mappings()).isNotNull();
    assertThat(c.mappings().stride()).contains("I");
    assertThat(c.mappings().capec()).contains(54, 113, 116);
    assertThat(c.mappings().owaspAsvs()).contains("2.4.1", "4.3.2", "13.2.2");
    assertThat(c.mappings().owaspDevGuide()).contains("SC1", "SC2", "SC3");
    // rich content from the mini data/cards tree
    assertThat(c.explanation()).contains("Brian");
    assertThat(c.explanation()).contains("Information Disclosure");
    assertThat(c.technicalNote()).contains("verbose error messages");
    // single location in the fixture
    assertThat(c.locations()).containsExactly("webapp:3.0:VE");
  }

  @Test
  void cardWithoutRichContentStillReturnsYamlAndMappings() {
    Optional<CardInfo> info = service.infoFor("AT2");
    assertThat(info).isPresent();
    CardInfo c = info.get();
    assertThat(c.description()).contains("credential");
    assertThat(c.mappings()).isNotNull();
    assertThat(c.mappings().stride()).contains("S");
    assertThat(c.mappings().capec()).contains(21, 652);
    assertThat(c.explanation()).isNull(); // no data/cards dir for AT in the fixture
    assertThat(c.technicalNote()).isNull();
  }

  @Test
  void unknownIdReturnsEmpty() {
    assertThat(service.infoFor("XX9")).isEmpty();
  }

  @Test
  void idPresentInSeveralSetsListsAllLocationsAndPrefersTheAtelierSet(@TempDir Path home)
      throws Exception {
    // build a tiny two-set repo where the same card id exists in both editions
    Path source = home.resolve("source");
    Files.createDirectories(source);
    Files.writeString(
        source.resolve("webapp-cards-3.0-en.yaml"),
        """
        meta: { edition: "webapp", component: "cards", language: "en", version: "3.0" }
        suits:
        - id: "VE"
          name: "DATA VALIDATION & ENCODING"
          cards:
          - { id: "VE2", value: "2", desc: "webapp desc" }
        """);
    Files.writeString(
        source.resolve("eop-cards-5.0-en.yaml"),
        """
        meta: { edition: "eop", component: "cards", language: "en", version: "5.0" }
        suits:
        - id: "SP"
          name: "Spoofing"
          cards:
          - { id: "VE2", value: "2", desc: "eop desc" }
        """);
    CardInfoService svc = new CardInfoService(home);
    CardInfo c = svc.infoFor("VE2").orElseThrow();
    assertThat(c.locations()).containsExactlyInAnyOrder("webapp:3.0:VE", "eop:5.0:SP");
    // without preference, one of them is primary (first by edition order); with preference, the
    // atelier's set wins
    CardInfo preferred = svc.infoFor("VE2", Set.of("eop")).orElseThrow();
    assertThat(preferred.setId()).isEqualTo("eop");
    assertThat(preferred.description()).isEqualTo("eop desc");
  }
}
