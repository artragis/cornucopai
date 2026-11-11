package com.cornucopai.game.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VoteDecisionModelTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void recordExposesAllFields() {
        VoteDecision d = new VoteDecision("Joueur-2", VoteChoice.NO, ExplanationCheck.OK,
                ApplicabilityCheck.NOT_APPLICABLE, "pas d'API REST dans le projet");
        assertThat(d.voter()).isEqualTo("Joueur-2");
        assertThat(d.vote()).isEqualTo(VoteChoice.NO);
        assertThat(d.explanationCheck()).isEqualTo(ExplanationCheck.OK);
        assertThat(d.applicabilityCheck()).isEqualTo(ApplicabilityCheck.NOT_APPLICABLE);
        assertThat(d.justification()).isEqualTo("pas d'API REST dans le projet");
    }

    @Test
    void rejectsNullEnumsAndBlankVoter() {
        assertThatThrownBy(() -> new VoteDecision("a", null, ExplanationCheck.OK, ApplicabilityCheck.OK, "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VoteDecision("a", VoteChoice.YES, null, ApplicabilityCheck.OK, "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VoteDecision(" ", VoteChoice.YES, ExplanationCheck.OK, ApplicabilityCheck.OK, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void roundTripsThroughJackson() throws Exception {
        VoteDecision d = new VoteDecision("Joueur-1", VoteChoice.YES, ExplanationCheck.OK,
                ApplicabilityCheck.OK, "scénario applicable");
        String json = mapper.writeValueAsString(d);
        VoteDecision read = mapper.readValue(json, VoteDecision.class);
        assertThat(read).isEqualTo(d);
        // JSON uses the enum names
        assertThat(json).contains("\"vote\":\"YES\"").contains("\"applicabilityCheck\":\"OK\"");
    }

    @Test
    void unknownEnumValueFailsDeserializationSoCallerFallsBack() {
        // a model emitting an unknown value must NOT deserialize silently
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                mapper.readValue("{\"voter\":\"a\",\"vote\":\"MAYBE\",\"explanationCheck\":\"OK\",\"applicabilityCheck\":\"OK\",\"justification\":\"\"}",
                        VoteDecision.class))
                .isInstanceOf(Exception.class);
    }
}
