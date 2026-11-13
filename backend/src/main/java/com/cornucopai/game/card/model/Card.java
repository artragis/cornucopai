package com.cornucopai.game.card.model;

import com.cornucopai.game.model.CardValueOrder;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.Objects;

/**
 * A real Cornucopia card (spec 003, R-016). Persisted inside {@code atelier.json} as part of the
 * {@code deck} (full object: id + description), and referenced by a leg through its real {@code
 * cardName} (= {@link #id}).
 */
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Card implements Comparable<Card> {

  private String id;
  private String value;
  private String setId; // edition, e.g. webapp
  private String suitId; // family id, e.g. VE
  private String suitName; // family name, e.g. DATA VALIDATION & ENCODING
  private String language; // resolved language of this card, e.g. fr / en
  private String description;
  private String url;
  private String misc;

  /** Jackson deserialization constructor. */
  public Card() {}

  public Card(
      String id,
      String value,
      String setId,
      String suitId,
      String suitName,
      String language,
      String description,
      String url,
      String misc) {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("card id must be non-empty");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("card description must be non-empty");
    }
    this.id = id;
    this.value = value;
    this.setId = setId;
    this.suitId = suitId;
    this.suitName = suitName;
    this.language = language;
    this.description = description;
    this.url = url;
    this.misc = misc;
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

  public void setId(String id) {
    this.id = id;
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

  public void setValue(String value) {
    this.value = value;
  }

  public void setSetId(String setId) {
    this.setId = setId;
  }

  public void setSuitId(String suitId) {
    this.suitId = suitId;
  }

  public void setSuitName(String suitName) {
    this.suitName = suitName;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public void setMisc(String misc) {
    this.misc = misc;
  }

  @Override
  public int compareTo(Card o) {
    if (!Objects.equals(o.suitId, suitId)) {
      return -1;
    }
    return CardValueOrder.compare(value, o.value);
  }
}
