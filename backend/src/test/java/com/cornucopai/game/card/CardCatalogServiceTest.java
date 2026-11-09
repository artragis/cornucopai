package com.cornucopai.game.card;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.card.model.CardSet;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CardCatalogServiceTest {

    // fixture repo root (contains source/); tests run with the backend module as working dir
    private static final Path FIXTURE_HOME = Path.of("src/test/resources/cornucopia-fixture");

    private CardCatalogService service() {
        return new CardCatalogService(FIXTURE_HOME);
    }

    @Test
    void keepsOnlyLatestVersionPerEdition() {
        // fixture has webapp 2.2 and 3.0 -> only 3.0 exposed; eop 5.0 -> 2 sets total
        List<CardSet> sets = service().catalog().sets();
        assertThat(sets).extracting(CardSet::id).containsExactlyInAnyOrder("webapp", "eop");
        assertThat(sets).extracting(CardSet::version).containsExactlyInAnyOrder("3.0", "5.0");
        assertThat(sets).noneMatch(s -> s.version().equals("2.2"));
    }

    @Test
    void listsLanguagesLowercaseAndParsesFamiliesFromEnFile() {
        CardSet webapp = service().set("webapp").orElseThrow();
        assertThat(webapp.languages()).containsExactlyInAnyOrder("en", "fr");
        // families parsed from the en file (structure is language-independent)
        assertThat(webapp.families()).extracting(f -> f.id()).containsExactlyInAnyOrder("VE", "AT", "WC");
        assertThat(webapp.families()).extracting(f -> f.cardCount())
                .containsExactlyInAnyOrder(2, 2, 1);
        assertThat(webapp.totalCards()).isEqualTo(5);

        CardSet eop = service().set("eop").orElseThrow();
        assertThat(eop.languages()).containsExactly("en"); // no fr for eop
    }

    @Test
    void availableLanguagesIsUnionOfAllSetsLowercase() {
        assertThat(service().availableLanguages()).containsExactlyInAnyOrder("en", "fr");
    }

    @Test
    void resolveLanguageUsesChosenLangWhenAvailableElseEnWithinSameVersion() {
        assertThat(service().resolveLanguage("webapp", "fr")).contains("fr");
        assertThat(service().resolveLanguage("eop", "fr")).contains("en"); // fallback, never an older version
        assertThat(service().resolveLanguage("webapp", "FR")).contains("fr"); // normalized lowercase (FR-018)
        assertThat(service().resolveLanguage("webapp", "fa")).contains("en"); // fa != fr
        assertThat(service().resolveLanguage("unknown", "fr")).isEmpty();
    }

    @Test
    void cardsForReturnsRealCardsInResolvedLanguageOnlyForSelectedFamilies() {
        List<Card> frVe = service().cardsFor("webapp", Set.of("VE"), "fr");
        assertThat(frVe).hasSize(2);
        assertThat(frVe).allMatch(c -> c.language().equals("fr"));
        assertThat(frVe).allMatch(c -> c.suitId().equals("VE"));
        assertThat(frVe).extracting(Card::id).containsExactlyInAnyOrder("VE2", "VE3");
        assertThat(frVe).allMatch(c -> c.description().contains("Robert") || c.description().contains("Dave"));
        assertThat(frVe.get(0).setId()).isEqualTo("webapp");
        assertThat(frVe.get(0).suitName()).isEqualTo("VALIDATION & ENCODAGE DES DONNEES");

        List<Card> enAt = service().cardsFor("webapp", Set.of("AT"), "en");
        assertThat(enAt).extracting(Card::id).containsExactlyInAnyOrder("AT2", "ATJ");
        assertThat(enAt).allMatch(c -> c.language().equals("en"));
        assertThat(enAt).allMatch(c -> c.description().contains("attacker"));
    }

    @Test
    void unknownFamilyIdIsNotKnown() {
        assertThat(service().isKnownFamily("webapp", "VE")).isTrue();
        assertThat(service().isKnownFamily("webapp", "ZZ")).isFalse();
        assertThat(service().isKnownFamily("eop", "SP")).isTrue();
    }

    @Test
    void blankOrMissingHomeYieldsEmptyCatalog() {
        assertThat(new CardCatalogService(Path.of("")).catalog().sets()).isEmpty();
        assertThat(new CardCatalogService(Path.of("src/test/resources/does-not-exist")).catalog().sets()).isEmpty();
        assertThat(new CardCatalogService(Path.of("")).availableLanguages()).isEmpty();
    }

    @Test
    void setUnknownReturnsEmpty() {
        assertThat(service().set("nope")).isEqualTo(Optional.empty());
    }
}
