package com.cornucopai.api;

import com.cornucopai.game.card.CardCatalog;
import com.cornucopai.game.card.CardCatalogService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * GET /api/card-catalog — the card catalog (sets, families, languages) used to populate the
 * hierarchical set → familles selector (FR-003, FR-014). Contract:
 * specs/003-card-set-selection/contracts/card-catalog-api.yaml.
 */
@Path("/api/card-catalog")
@Produces(MediaType.APPLICATION_JSON)
public class CardCatalogResource {

  @Inject CardCatalogService catalogService;

  @GET
  public CardCatalog getCardCatalog() {
    return catalogService.catalog();
  }
}
