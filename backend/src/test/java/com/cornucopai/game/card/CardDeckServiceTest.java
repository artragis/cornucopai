package com.cornucopai.game.card;

import static org.assertj.core.api.Assertions.assertThat;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.model.SetResolution;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CardDeckServiceTest {

  private static final Path FIXTURE_HOME = Path.of("src/test/resources/cornucopia-fixture");

  private CardDeckService service() {
    return new CardDeckService(new CardCatalogService(FIXTURE_HOME));
  }

  @Test
  void deckContainsOnlySelectedFamiliesAcrossSetsWithRealIdsAndDescriptions() {
    CardDeckService.DeckResult result =
        service().buildDeck(List.of("webapp:VE", "eop:SP"), "fr", new Random(42));
    assertThat(result.deck()).hasSize(4); // VE2, VE3, SP2, SPA
    assertThat(result.deck())
        .extracting(Card::id)
        .containsExactlyInAnyOrder("VE2", "VE3", "SP2", "SPA");
    assertThat(result.deck()).allMatch(c -> c.description() != null && !c.description().isBlank());
  }

  @Test
  void perSetLanguageResolvesToFrWhenAvailableElseEn() {
    CardDeckService.DeckResult result =
        service().buildDeck(List.of("webapp:VE", "eop:SP"), "fr", new Random(1));
    assertThat(result.sets())
        .containsExactlyInAnyOrder(
            new SetResolution("webapp", "fr"), new SetResolution("eop", "en"));
    // webapp cards in French, eop cards in English (fallback)
    assertThat(result.deck().stream().filter(c -> c.setId().equals("webapp")))
        .allMatch(c -> c.language().equals("fr"));
    assertThat(result.deck().stream().filter(c -> c.setId().equals("eop")))
        .allMatch(c -> c.language().equals("en"));
  }

  @Test
  void fallbackNeverUsesAnOlderVersion() {
    // webapp 2.2 also exists in the fixture but must be ignored; fr exists only in 3.0
    CardDeckService.DeckResult result =
        service().buildDeck(List.of("webapp:VE"), "fr", new Random(1));
    assertThat(result.sets()).containsExactly(new SetResolution("webapp", "fr"));
    assertThat(result.deck())
        .allMatch(c -> c.description().contains("Robert") || c.description().contains("Dave"));
  }

  @Test
  void shuffleIsDeterministicForGivenSeed() {
    List<Card> a =
        service().buildDeck(List.of("webapp:VE", "webapp:AT"), "en", new Random(7)).deck();
    List<Card> b =
        service().buildDeck(List.of("webapp:VE", "webapp:AT"), "en", new Random(7)).deck();
    assertThat(a).extracting(Card::id).isEqualTo(b.stream().map(Card::id).toList());
  }

  @Test
  void duplicateFamilyIsNotDuplicated() {
    CardDeckService.DeckResult result =
        service().buildDeck(List.of("webapp:VE", "webapp:VE"), "en", new Random(1));
    assertThat(result.deck()).extracting(Card::id).containsExactlyInAnyOrder("VE2", "VE3");
  }

  @Test
  void languageCodeIsNormalizedLowercase() {
    CardDeckService.DeckResult result =
        service().buildDeck(List.of("webapp:VE"), "FR", new Random(1));
    assertThat(result.sets()).containsExactly(new SetResolution("webapp", "fr"));
  }
}
