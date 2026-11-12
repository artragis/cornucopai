package com.cornucopai.game.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * One played card in a Cornucopia game (a "tour"). Persisted as its own JSON file with exactly 5
 * fields (contracts/leg-schema.json): cardName, scenario, explanation, entries, validation.
 *
 * <p>Spec 005 v2: {@code entries} (list of structured {@link LegEntry}) replaces the v1 flat {@code
 * precisions} string list (R-036). Legacy v1 files are still readable: the explicitly annotated
 * {@link #setLegacyPrecisions(List)} converts incoming {@code precisions} strings into PRECISION
 * entries (author {@code "inconnu"}) when {@code entries} is empty. {@code precisions} is never
 * written anymore.
 */
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Leg {

  private String cardName;
  private String scenario;
  private String explanation;
  private List<LegEntry> entries;
  private ValidationStatus validation;
  // Spec 006 v3 (R-047): additive tour/score fields. Numeric fields are primitive int:
  // Jackson defaults a missing JSON field to 0 on legacy reads, and 0 already means
  // "no tour / no points". String fields stay nullable (null = absent in legacy files).
  private int turnIndex;
  private String majorSuit;
  private String suitId;
  private String cardValue;
  private int points;
  // Spec 007 v4 (R-053): per-voter structured vote decisions — empty on legacy legs
  private List<VoteDecision> votes = new ArrayList<>();

  /** Jackson deserialization constructor. */
  public Leg() {
    this.entries = new ArrayList<>();
    this.validation = ValidationStatus.PENDING;
  }

  public Leg(String cardName, String scenario, String explanation) {
    if (cardName == null || cardName.isBlank()) {
      throw new IllegalArgumentException("cardName must be non-empty");
    }
    if (scenario == null || scenario.isBlank()) {
      throw new IllegalArgumentException("scenario must be non-empty");
    }
    if (explanation == null || explanation.isBlank()) {
      throw new IllegalArgumentException("explanation must be non-empty");
    }
    this.cardName = cardName;
    this.scenario = scenario;
    this.explanation = explanation;
    this.entries = new ArrayList<>();
    this.validation = ValidationStatus.PENDING;
  }

  public void addEntry(LegEntry entry) {
    if (entry == null) {
      throw new IllegalArgumentException("entry must not be null");
    }
    this.entries.add(entry);
  }

  /**
   * Legacy v1 read compatibility (FR-003, R-036): converts each incoming {@code precisions} string
   * into a PRECISION entry with author {@code "inconnu"}. The explicit {@code @JsonProperty}
   * overrides the class-level {@code setterVisibility = NONE}; there is no field/getter named
   * {@code precisions}, so it is never serialized.
   */
  @JsonProperty("precisions")
  public void setLegacyPrecisions(List<String> precisions) {
    if (precisions == null || !this.entries.isEmpty()) {
      return;
    }
    for (String p : precisions) {
      if (p != null && !p.isBlank()) {
        this.entries.add(LegEntry.precision("inconnu", p));
      }
    }
  }

  public String cardName() {
    return cardName;
  }

  public String scenario() {
    return scenario;
  }

  public String explanation() {
    return explanation;
  }

  public List<LegEntry> entries() {
    return entries;
  }

  public ValidationStatus validation() {
    return validation;
  }

  public void setCardName(String cardName) {
    this.cardName = cardName;
  }

  public void setScenario(String scenario) {
    this.scenario = scenario;
  }

  public void setExplanation(String explanation) {
    this.explanation = explanation;
  }

  public void setEntries(List<LegEntry> entries) {
    this.entries = entries;
  }

  public void setValidation(ValidationStatus validation) {
    this.validation = validation;
  }

  // ---- spec 006 v3 accessors (leg-schema.json v3.0.0) ----

  public int turnIndex() {
    return turnIndex;
  }

  public String majorSuit() {
    return majorSuit;
  }

  public String suitId() {
    return suitId;
  }

  public String cardValue() {
    return cardValue;
  }

  public int points() {
    return points;
  }

  public void setTurnIndex(int turnIndex) {
    this.turnIndex = turnIndex;
  }

  public void setMajorSuit(String majorSuit) {
    this.majorSuit = majorSuit;
  }

  public void setSuitId(String suitId) {
    this.suitId = suitId;
  }

  public void setCardValue(String cardValue) {
    this.cardValue = cardValue;
  }

  public void setPoints(int points) {
    this.points = points;
  }

  // ---- spec 007 v4 accessors (leg-schema.json v4.0.0) ----

  public List<VoteDecision> votes() {
    return votes;
  }

  public void setVotes(List<VoteDecision> votes) {
    this.votes = votes == null ? new ArrayList<>() : votes;
  }
}
