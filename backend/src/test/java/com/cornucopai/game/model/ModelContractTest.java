package com.cornucopai.game.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelContractTest {

    @Test
    void validationStatusHasExactlyThreeValues() {
        assertThat(ValidationStatus.values())
                .containsExactlyInAnyOrder(ValidationStatus.PENDING, ValidationStatus.VALIDATED, ValidationStatus.REJECTED);
    }

    @Test
    void archetypeHasExactlyFourValues() {
        assertThat(Archetype.values()).containsExactlyInAnyOrder(
                Archetype.GRUMPY, Archetype.HAPPY, Archetype.GREEDY, Archetype.PROF);
    }

    @Test
    void voteHasExactlyYesAndNo() {
        assertThat(Vote.values()).containsExactlyInAnyOrder(Vote.YES, Vote.NO);
    }

    @Test
    void legRequiresNonEmptyCardScenarioAndExplanation() {
        assertThatThrownBy(() -> new Leg("", "s", "e")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Leg("c", "", "e")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Leg("c", "s", "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Leg(null, "s", "e")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void legStartsPendingWithEmptyEntriesAndIsUpdatable() {
        Leg leg = new Leg("Carte-Placeholder-1", "scenario", "explanation");
        assertThat(leg.validation()).isEqualTo(ValidationStatus.PENDING);
        assertThat(leg.entries()).isEmpty();

        leg.addEntry(LegEntry.precision("Joueur-1", "une précision"));
        assertThat(leg.entries()).hasSize(1);
        assertThat(leg.entries().get(0).kind()).isEqualTo(LegEntry.Kind.PRECISION);

        leg.setValidation(ValidationStatus.VALIDATED);
        assertThat(leg.validation()).isEqualTo(ValidationStatus.VALIDATED);
    }

    @Test
    void reactionVariants() {
        Reaction precision = new Reaction.Precision("text");
        Reaction question = new Reaction.Question("text");
        Reaction pass = new Reaction.Pass();
        assertThat(precision).isInstanceOf(Reaction.Precision.class);
        assertThat(question).isInstanceOf(Reaction.Question.class);
        assertThat(pass).isInstanceOf(Reaction.Pass.class);
    }

    @Test
    void answerVariants() {
        assertThat(new Answer.Knows("réponse")).isInstanceOf(Answer.Knows.class);
        assertThat(new Answer.DoesNotKnow()).isInstanceOf(Answer.DoesNotKnow.class);
    }

    @Test
    void participantExposesNameAndArchetype() {
        Participant p = new Participant("Joueur-1", Archetype.GRUMPY);
        assertThat(p.name()).isEqualTo("Joueur-1");
        assertThat(p.archetype()).isEqualTo(Archetype.GRUMPY);
    }
}
