package com.cornucopai.game.card.model;

import java.util.List;

/**
 * Everything interesting about one Cornucopia card, aggregated by {@code CardInfoService} from the
 * three sources of R-024: the cards YAML (id, value, description, url, misc, family, set), the
 * mappings YAML ({@link CardMappings}), and the rich markdown under {@code
 * cornucopia.owasp.org/data/cards/} (explanation, technicalNote). {@code locations} lists every
 * {@code setId:version:suitId} where the id exists (FR-008); rich content and mappings may be null
 * when absent upstream.
 */
public class CardInfo {

  private final String id;
  private final String value;
  private final String setId;
  private final String version;
  private final String suitId;
  private final String suitName;
  private final String language;
  private final String description;
  private final String url;
  private final String misc;
  private final CardMappings mappings;
  private final String explanation;
  private final String technicalNote;
  private final List<String> locations;

  public CardInfo(
      String id,
      String value,
      String setId,
      String version,
      String suitId,
      String suitName,
      String language,
      String description,
      String url,
      String misc,
      CardMappings mappings,
      String explanation,
      String technicalNote,
      List<String> locations) {
    this.id = id;
    this.value = value;
    this.setId = setId;
    this.version = version;
    this.suitId = suitId;
    this.suitName = suitName;
    this.language = language;
    this.description = description;
    this.url = url;
    this.misc = misc;
    this.mappings = mappings;
    this.explanation = explanation;
    this.technicalNote = technicalNote;
    this.locations = locations;
  }

  public String id() {
    return id;
  }

  public String value() {
    return value;
  }

  public String setId() {
    return setId;
  }

  public String version() {
    return version;
  }

  public String suitId() {
    return suitId;
  }

  public String suitName() {
    return suitName;
  }

  public String language() {
    return language;
  }

  public String description() {
    return description;
  }

  public String url() {
    return url;
  }

  public String misc() {
    return misc;
  }

  public CardMappings mappings() {
    return mappings;
  }

  public String explanation() {
    return explanation;
  }

  public String technicalNote() {
    return technicalNote;
  }

  public List<String> locations() {
    return locations;
  }
}
