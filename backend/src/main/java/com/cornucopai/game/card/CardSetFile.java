package com.cornucopai.game.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Jackson POJO mapping of a Cornucopia {@code *-cards-*.yaml} file (R-011). Parsed with a Jackson
 * {@code ObjectMapper} built on {@code YAMLFactory}.
 *
 * <pre>
 * meta: { edition, component, language, version }
 * suits:
 *   - id, name, cards: [ { id, value, desc, url?, misc? } ]
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardSetFile {

  private Meta meta;
  private List<SuitEntry> suits;

  public Meta getMeta() {
    return meta;
  }

  public void setMeta(Meta meta) {
    this.meta = meta;
  }

  public List<SuitEntry> getSuits() {
    return suits;
  }

  public void setSuits(List<SuitEntry> suits) {
    this.suits = suits;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Meta {
    private String edition;
    private String component;
    private String language;
    private String version;

    public String getEdition() {
      return edition;
    }

    public void setEdition(String edition) {
      this.edition = edition;
    }

    public String getComponent() {
      return component;
    }

    public void setComponent(String component) {
      this.component = component;
    }

    public String getLanguage() {
      return language;
    }

    public void setLanguage(String language) {
      this.language = language;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class SuitEntry {
    private String id;
    private String name;
    private List<CardEntry> cards;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<CardEntry> getCards() {
      return cards;
    }

    public void setCards(List<CardEntry> cards) {
      this.cards = cards;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CardEntry {
    private String id;
    private String value;
    private String desc;
    private String url;
    private String misc;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public String getDesc() {
      return desc;
    }

    public void setDesc(String desc) {
      this.desc = desc;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getMisc() {
      return misc;
    }

    public void setMisc(String misc) {
      this.misc = misc;
    }
  }
}
