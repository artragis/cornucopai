package com.cornucopai.game.model;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.llm.AgentLlmConfig;
import com.cornucopai.game.llm.AgentSnapshot;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A game session (workshop). Persisted as {@code atelier.json} inside its {@code
 * atelier{yyyyMMdd-HHmmss}} folder (research R-002).
 *
 * <p>Spec 003: {@code deck} is now a list of real {@link Card} objects (id + description persisted
 * here, R-016), and the atelier also records the chosen {@code language}, the {@code
 * selectedFamilies} ({@code setId:familyId}) and the per-set {@code sets} resolved language.
 *
 * <p>Spec 004: {@code agentConfigs} persists the resolved per-player LLM provider configs (FR-012),
 * index-aligned with {@code players} — legacy, still readable, no longer written.
 *
 * <p>Spec 009: {@code agentSnapshots} persists a full copy of each player's agent config at launch
 * (FR-017), index-aligned with {@code players}; {@code players[]} gains {@code agentId}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Atelier {

  private String id;
  private Instant createdAt;
  private String featureHtml;
  private int playerCount;
  private List<Participant> players;
  private List<String> turnOrder;
  private List<Card> deck;
  private Instant finishedAt;
  private String language = "fr";
  private List<String> selectedFamilies = new ArrayList<>();
  private List<SetResolution> sets = new ArrayList<>();

  /**
   * Spec 004 field, readable from legacy ateliers, no longer written.
   *
   * @deprecated spec 009 (FR-014): use {@link #agentSnapshots()}.
   */
  @Deprecated private List<AgentLlmConfig> agentConfigs = new ArrayList<>();

  // Spec 009 (FR-017): snapshot of each player's agent at launch, index-aligned with players
  private List<AgentSnapshot> agentSnapshots = new ArrayList<>();
  // Spec 006 (R-048): tours, per-player scores, winner — empty on legacy ateliers
  private List<Tour> tours = new ArrayList<>();
  private Map<String, Integer> scores = new LinkedHashMap<>();
  private List<String> winner = new ArrayList<>();
  // Spec 007 (R-053): shared project memory — empty on legacy ateliers
  private List<com.cornucopai.game.memory.ProjectFact> projectFacts = new ArrayList<>();

  /** Jackson deserialization constructor. */
  public Atelier() {}

  public Atelier(
      String id,
      Instant createdAt,
      String featureHtml,
      int playerCount,
      List<Participant> players,
      List<String> turnOrder,
      List<Card> deck) {
    this.id = id;
    this.createdAt = createdAt;
    this.featureHtml = featureHtml;
    this.playerCount = playerCount;
    this.players = players;
    this.turnOrder = turnOrder;
    this.deck = deck;
  }

  public String id() {
    return id;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public String featureHtml() {
    return featureHtml;
  }

  public int playerCount() {
    return playerCount;
  }

  public List<Participant> players() {
    return players;
  }

  public List<String> turnOrder() {
    return turnOrder;
  }

  public List<Card> deck() {
    return deck;
  }

  public Instant finishedAt() {
    return finishedAt;
  }

  public String language() {
    return language;
  }

  public List<String> selectedFamilies() {
    return selectedFamilies;
  }

  public List<SetResolution> sets() {
    return sets;
  }

  /**
   * Legacy getter (spec 004).
   *
   * @deprecated spec 009 (FR-014), see {@link #agentSnapshots()}.
   */
  @Deprecated
  public List<AgentLlmConfig> agentConfigs() {
    return agentConfigs;
  }

  public List<AgentSnapshot> agentSnapshots() {
    return agentSnapshots;
  }

  public void setAgentSnapshots(List<AgentSnapshot> agentSnapshots) {
    this.agentSnapshots = agentSnapshots == null ? new ArrayList<>() : agentSnapshots;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public void setFeatureHtml(String featureHtml) {
    this.featureHtml = featureHtml;
  }

  public void setPlayerCount(int playerCount) {
    this.playerCount = playerCount;
  }

  public void setPlayers(List<Participant> players) {
    this.players = players;
  }

  public void setTurnOrder(List<String> turnOrder) {
    this.turnOrder = turnOrder;
  }

  public void setDeck(List<Card> deck) {
    this.deck = deck;
  }

  public void setFinishedAt(Instant finishedAt) {
    this.finishedAt = finishedAt;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public void setSelectedFamilies(List<String> selectedFamilies) {
    this.selectedFamilies = selectedFamilies;
  }

  public void setSets(List<SetResolution> sets) {
    this.sets = sets;
  }

  /**
   * Legacy setter (spec 004), kept so old tests/fixtures can populate it.
   *
   * @deprecated spec 009 (FR-014).
   */
  @Deprecated
  public void setAgentConfigs(List<AgentLlmConfig> agentConfigs) {
    this.agentConfigs = agentConfigs;
  }

  // ---- spec 006 accessors (ateliers-api v0.6.0) ----

  public List<Tour> tours() {
    return tours;
  }

  public Map<String, Integer> scores() {
    return scores;
  }

  public List<String> winner() {
    return winner;
  }

  public void setTours(List<Tour> tours) {
    this.tours = tours;
  }

  public void setScores(Map<String, Integer> scores) {
    this.scores = scores;
  }

  public void setWinner(List<String> winner) {
    this.winner = winner;
  }

  // ---- spec 007 accessors (ateliers-api v0.7.0, atelier-schema v0.7.0) ----

  public List<com.cornucopai.game.memory.ProjectFact> projectFacts() {
    return projectFacts;
  }

  public void setProjectFacts(List<com.cornucopai.game.memory.ProjectFact> projectFacts) {
    this.projectFacts = projectFacts == null ? new ArrayList<>() : projectFacts;
  }
}
