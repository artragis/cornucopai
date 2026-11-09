package com.cornucopai.game.card.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardModelContractTest {

    @Test
    void cardRequiresNonEmptyIdentityAndDescription() {
        assertThatThrownBy(() -> new Card("", "2", "webapp", "VE", "VE name", "en", "desc", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Card("VE2", "2", "webapp", "VE", "VE name", "en", "  ", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        Card card = new Card("VE2", "2", "webapp", "VE", "DATA VALIDATION & ENCODING", "fr",
                "Robert peut saisir...", "https://cornucopia.owasp.org/cards/VE2", null);
        assertThat(card.id()).isEqualTo("VE2");
        assertThat(card.setId()).isEqualTo("webapp");
        assertThat(card.suitId()).isEqualTo("VE");
        assertThat(card.language()).isEqualTo("fr");
        assertThat(card.description()).contains("Robert");
    }

    @Test
    void cardSetAndCardFamilyShape() {
        CardFamily family = new CardFamily("VE", "DATA VALIDATION & ENCODING", "webapp", 2);
        CardSet set = new CardSet("webapp", "3.0", List.of("en", "fr"), List.of(family), 2);
        assertThat(set.id()).isEqualTo("webapp");
        assertThat(set.version()).isEqualTo("3.0");
        assertThat(set.languages()).containsExactly("en", "fr");
        assertThat(set.families()).containsExactly(family);
        assertThat(set.totalCards()).isEqualTo(2);
        assertThat(family.cardCount()).isEqualTo(2);
    }

    @Test
    void globalFamilyIdIsSetIdColonFamilyId() {
        // spec: a family is identified by the pair setId:familyId, e.g. webapp:VE
        CardFamily family = new CardFamily("VE", "DATA VALIDATION & ENCODING", "webapp", 2);
        assertThat(family.setId() + ":" + family.id()).isEqualTo("webapp:VE");
    }
}
