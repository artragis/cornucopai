package com.cornucopai.game.card;

import com.cornucopai.game.card.model.CardInfo;
import com.cornucopai.game.card.model.CardMappings;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.jboss.logging.Logger;

/**
 * The first tool of the agents' LangChain4j toolbox (spec 004 FR-005/FR-006, R-023): {@code
 * get_card_info(cardId)} returns everything interesting about a Cornucopia card from its id alone,
 * rendered as Markdown. Unknown id → explicit "Carte inconnue" (FR-009). Registered on every {@code
 * LangChain4jBrain} via {@code LlmBrainFactory}.
 */
@ApplicationScoped
public class CardInfoTool {

  private static final Logger LOG = Logger.getLogger(CardInfoTool.class);

  private final CardInfoService service;

  @Inject
  public CardInfoTool(CardInfoService service) {
    this.service = service;
  }

  @Tool(
      name = "get_card_info",
      value =
          """
          Retourne toutes les informations intéressantes sur une carte Cornucopia à partir de son \
          identifiant (ex. VE2) : valeur, famille, description, URL, mappings de sécurité (STRIDE, \
          CAPEC, ASVS, OWASP Dev Guide, SAFECode) et contenu riche (scénario d'exemple, note technique). \
          Utilise cet outil dès que tu dois raisonner en profondeur sur une carte.\
          """)
  public String getCardInfo(@P("Identifiant de la carte, ex. VE2") String cardId) {
    Optional<CardInfo> info = service.infoFor(cardId);
    // structured log of the tool call (constitution IV) — the card id is not sensitive
    LOG.infof("tool get_card_info cardId=%s found=%b", cardId, info.isPresent());
    if (info.isEmpty()) {
      return "Carte inconnue : " + cardId;
    }
    return toMarkdown(info.get());
  }

  static String toMarkdown(CardInfo c) {
    StringBuilder md = new StringBuilder();
    md.append("# Carte ").append(c.id());
    if (c.value() != null) {
      md.append(" (valeur ").append(c.value()).append(")");
    }
    md.append("\n\n");
    md.append("- Set : ")
        .append(c.setId())
        .append(" ")
        .append(c.version())
        .append(" (langue ")
        .append(c.language())
        .append(")\n");
    md.append("- Famille : ").append(c.suitId()).append(" — ").append(c.suitName()).append("\n");
    if (!c.locations().isEmpty()) {
      md.append("- Localisations : ").append(String.join(", ", c.locations())).append("\n");
    }
    if (c.url() != null) {
      md.append("- URL : ").append(c.url()).append("\n");
    }
    md.append("\n## Description\n").append(c.description()).append("\n");
    if (c.misc() != null) {
      md.append("\n## Note diverses\n").append(c.misc()).append("\n");
    }
    CardMappings m = c.mappings();
    if (m != null) {
      md.append("\n## Mappings de sécurité\n");
      appendList(md, "STRIDE", firstNonNull(m.stridePrint(), m.stride()));
      appendList(md, "CAPEC", m.capec());
      appendList(md, "OWASP ASVS", firstNonNull(m.owaspAsvsPrint(), m.owaspAsvs()));
      appendList(md, "OWASP Dev Guide", firstNonNull(m.owaspDevGuidePrint(), m.owaspDevGuide()));
      appendList(md, "SAFECode", m.safecode());
    }
    if (c.explanation() != null) {
      md.append("\n## Explication (contenu riche)\n").append(c.explanation()).append("\n");
    }
    if (c.technicalNote() != null) {
      md.append("\n## Note technique\n").append(c.technicalNote()).append("\n");
    }
    return md.toString();
  }

  private static void appendList(StringBuilder md, String label, List<Object> values) {
    if (values == null || values.isEmpty()) {
      return;
    }
    md.append("- ").append(label).append(" : ");
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        md.append(", ");
      }
      md.append(values.get(i));
    }
    md.append("\n");
  }

  private static List<Object> firstNonNull(List<Object> a, List<Object> b) {
    return a != null && !a.isEmpty() ? a : b;
  }
}
