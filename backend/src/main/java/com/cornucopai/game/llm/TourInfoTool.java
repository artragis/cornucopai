package com.cornucopai.game.llm;

import com.cornucopai.game.model.Leg;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import org.jboss.logging.Logger;

/**
 * The per-game toolbox tool {@code get_tour_info} (spec 005 US-2, FR-004, R-038): returns the
 * current leg as JSON — cardName, scenario, explanation, and the structured entries (precisions and
 * questions with their {@code knows}/{@code answer}) — so the model can cheaply see whether a
 * question already got « Je sais pas », without the whole leg being copied into its prompt. Returns
 * {@code "Aucun tour en cours"} when no leg is on the table.
 */
public class TourInfoTool {

  private static final Logger LOG = Logger.getLogger(TourInfoTool.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final LegContext context;

  public TourInfoTool(LegContext context) {
    this.context = context;
  }

  @Tool(
      name = "get_tour_info",
      value =
          """
          Retourne le tour de Cornucopia en cours : la carte posée, son scénario, son explication, et la liste \
          structurée des entrées du tour (précisions, et questions déjà posées avec knows=true/false et leur \
          réponse). Utilise cet outil pour consulter le détail du tour en cours, notamment pour savoir si une \
          question a déjà reçu « Je sais pas » avant d'en poser une nouvelle.\
          """)
  public String getTourInfo() {
    Leg leg = context.current();
    LOG.infof(
        "tool get_tour_info atelier=%s leg=%s",
        context.atelierId(), leg == null ? "none" : leg.cardName());
    if (leg == null) {
      return "Aucun tour en cours";
    }
    try {
      return MAPPER.writeValueAsString(leg);
    } catch (JsonProcessingException e) {
      LOG.warnf(e, "Cannot serialize current leg");
      return "Erreur de lecture du tour en cours";
    }
  }
}
