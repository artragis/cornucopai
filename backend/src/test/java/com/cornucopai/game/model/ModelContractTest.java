package com.cornucopai.game.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModelContractTest {

  @Test
  void validationStatusHasExactlyThreeValues() {
    assertThat(ValidationStatus.values())
        .containsExactlyInAnyOrder(
            ValidationStatus.PENDING, ValidationStatus.VALIDATED, ValidationStatus.REJECTED);
  }

  @Test
  void archetypeHasExactlyFourValues() {
    assertThat(Archetype.values())
        .containsExactlyInAnyOrder(
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

  // T021 (spec 009): Participant carries the driving agent id; legacy archetype tolerated on read
  @Test
  void participantExposesNameAndAgentId() throws Exception {
    Participant p = new Participant("Joueur-1", "agent-42");
    assertThat(p.name()).isEqualTo("Joueur-1");
    assertThat(p.agentId()).isEqualTo("agent-42");
    assertThat(p.archetype()).isNull();
    // new writes never serialize archetype (NON_NULL)
    String json = mapper.writeValueAsString(p);
    assertThat(json).doesNotContain("archetype");
    // legacy JSON with archetype still deserializes (FR-014)
    Participant legacy =
        mapper.readValue("{\"name\":\"Joueur-9\",\"archetype\":\"HAPPY\"}", Participant.class);
    assertThat(legacy.archetype()).isEqualTo(Archetype.HAPPY);
    assertThat(legacy.agentId()).isNull();
  }

  @Test
  void atelierGainsAgentSnapshotsAndLegacyReadsEmpty() throws Exception {
    Atelier a = new Atelier();
    assertThat(a.agentSnapshots()).isEmpty();
    Atelier legacy = mapper.readValue("{\"id\":\"x\",\"agentConfigs\":[]}", Atelier.class);
    assertThat(legacy.agentSnapshots()).isEmpty();
    assertThat(legacy.agentConfigs()).isEmpty();
  }

  // ---- T004 (spec 006, Phase 2): Leg v3 additive fields + Atelier extended (R-047, R-048) ----

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void legV3RoundTripsWithTourFields() throws Exception {
    Leg leg = new Leg("VE2", "scenario", "explanation");
    leg.setTurnIndex(1);
    leg.setMajorSuit("VE");
    leg.setSuitId("VE");
    leg.setCardValue("7");
    leg.setPoints(2);
    String json = mapper.writeValueAsString(leg);
    Leg read = mapper.readValue(json, Leg.class);
    assertThat(read.turnIndex()).isEqualTo(1);
    assertThat(read.majorSuit()).isEqualTo("VE");
    assertThat(read.suitId()).isEqualTo("VE");
    assertThat(read.cardValue()).isEqualTo("7");
    assertThat(read.points()).isEqualTo(2);
    // original v2 fields still present
    assertThat(read.cardName()).isEqualTo("VE2");
    assertThat(read.entries()).isEmpty();
    assertThat(read.validation()).isEqualTo(ValidationStatus.PENDING);
  }

  @Test
  void legV3LegacyFileReadsWithDefaults() throws Exception {
    // a v2 leg file without the new fields (FR-017): numeric fields default to 0
    // (Jackson primitive default), string fields stay null (absent)
    String legacyJson =
        "{\"cardName\":\"VE2\",\"scenario\":\"s\",\"explanation\":\"e\",\"entries\":[],"
            + "\"validation\":\"VALIDATED\"}";
    Leg read = mapper.readValue(legacyJson, Leg.class);
    assertThat(read.turnIndex()).isZero();
    assertThat(read.points()).isZero();
    assertThat(read.majorSuit()).isNull();
    assertThat(read.suitId()).isNull();
    assertThat(read.cardValue()).isNull();
    assertThat(read.validation()).isEqualTo(ValidationStatus.VALIDATED);
  }

  @Test
  void atelierExtendedRoundTripsWithToursScoresWinner() throws Exception {
    Atelier a = new Atelier("atelierX", null, "<p>f</p>", 2, List.of(), List.of(), List.of());
    a.setTours(List.of(new Tour(1, "Joueur-1", "VE", List.of(1, 2), "Joueur-1", true)));
    a.setScores(Map.of("Joueur-1", 3, "Joueur-2", 1));
    a.setWinner(List.of("Joueur-1"));
    String json = mapper.writeValueAsString(a);
    Atelier read = mapper.readValue(json, Atelier.class);
    assertThat(read.tours()).hasSize(1);
    assertThat(read.tours().get(0).majorSuit()).isEqualTo("VE");
    assertThat(read.scores()).containsEntry("Joueur-1", 3);
    assertThat(read.winner()).containsExactly("Joueur-1");
  }

  @Test
  void atelierLegacyFileReadsWithEmptyTourFields() throws Exception {
    // a pre-006 atelier.json without tours/scores/winner (FR-017)
    String legacyJson =
        "{\"id\":\"atelierOld\",\"createdAt\":null,\"featureHtml\":\"<p>x</p>\",\"playerCount\":2,"
            + "\"players\":[],\"turnOrder\":[],\"deck\":[]}";
    Atelier read = mapper.readValue(legacyJson, Atelier.class);
    assertThat(read.tours()).isEmpty();
    assertThat(read.scores()).isEmpty();
    assertThat(read.winner()).isEmpty();
  }

  // ---- T004 (spec 007, Phase 2): Atelier.projectFacts + Leg.votes, additive v4.0.0/v0.7.0 (R-053)
  // ----

  @Test
  void atelierV070RoundTripsWithProjectFacts() throws Exception {
    Atelier a = new Atelier("atelierX", null, "<p>f</p>", 2, List.of(), List.of(), List.of());
    a.setProjectFacts(
        List.of(
            new com.cornucopai.game.memory.ProjectFact(
                "pas d'API REST", "Joueur-1", 1, java.time.Instant.parse("2026-11-11T10:00:00Z"))));
    String json = mapper.writeValueAsString(a);
    Atelier read = mapper.readValue(json, Atelier.class);
    assertThat(read.projectFacts()).hasSize(1);
    assertThat(read.projectFacts().get(0).text()).isEqualTo("pas d'API REST");
  }

  @Test
  void atelierLegacyWithoutProjectFactsReadsEmpty() throws Exception {
    String legacyJson =
        "{\"id\":\"atelierOld\",\"createdAt\":null,\"featureHtml\":\"<p>x</p>\",\"playerCount\":2,"
            + "\"players\":[],\"turnOrder\":[],\"deck\":[]}";
    Atelier read = mapper.readValue(legacyJson, Atelier.class);
    assertThat(read.projectFacts()).isEmpty();
  }

  @Test
  void legV4RoundTripsWithVotes() throws Exception {
    Leg leg = new Leg("VE2", "scenario", "explanation");
    leg.setVotes(
        List.of(
            new VoteDecision(
                "Joueur-2",
                com.cornucopai.game.model.VoteChoice.NO,
                com.cornucopai.game.model.ExplanationCheck.OK,
                com.cornucopai.game.model.ApplicabilityCheck.NOT_APPLICABLE,
                "hors sujet")));
    String json = mapper.writeValueAsString(leg);
    Leg read = mapper.readValue(json, Leg.class);
    assertThat(read.votes()).hasSize(1);
    assertThat(read.votes().get(0).vote()).isEqualTo(com.cornucopai.game.model.VoteChoice.NO);
    assertThat(read.votes().get(0).applicabilityCheck())
        .isEqualTo(com.cornucopai.game.model.ApplicabilityCheck.NOT_APPLICABLE);
  }

  @Test
  void legV4LegacyFileReadsWithEmptyVotes() throws Exception {
    String legacyJson =
        "{\"cardName\":\"VE2\",\"scenario\":\"s\",\"explanation\":\"e\",\"entries\":[],"
            + "\"validation\":\"PENDING\"}";
    Leg read = mapper.readValue(legacyJson, Leg.class);
    assertThat(read.votes()).isEmpty();
  }
}
