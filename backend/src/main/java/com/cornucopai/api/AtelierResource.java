package com.cornucopai.api;

import com.cornucopai.api.dto.AtelierResponse;
import com.cornucopai.api.dto.AtelierSummary;
import com.cornucopai.api.dto.CreateAtelierRequest;
import com.cornucopai.api.dto.ErrorResponse;
import com.cornucopai.game.AgentStore;
import com.cornucopai.game.AtelierService;
import com.cornucopai.game.card.CardCatalogService;
import com.cornucopai.game.llm.AgentConfigResolver;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

// Spec 009: POST consumes agentIds[] (API v0.8.0); the FR-013 env-var check happens at launch
// (LlmBrainFactory), which surfaces as 400 through AtelierService's IllegalArgumentException.

/**
 * REST contract: specs/002-cornucopia-game-flow/contracts/ateliers-api.yaml. POST runs the whole
 * game synchronously (research R-003).
 */
@Path("/api/ateliers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AtelierResource {

  @Inject AtelierService service;

  @Inject CardCatalogService catalogService;

  @Inject AgentStore agentStore;

  @POST
  public Response create(CreateAtelierRequest request) {
    // Spec 009 (FR-010): at least two selected agents, one per participant
    if (request == null || request.agentIds() == null || request.agentIds().size() < 2) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ErrorResponse("agentIds must contain at least 2 entries"))
          .build();
    }
    if (request.featureHtml() == null || request.featureHtml().isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ErrorResponse("featureHtml must be a non-empty WYSIWYG content"))
          .build();
    }
    // FR-006: at least one family selected
    if (request.families() == null || request.families().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ErrorResponse("families must contain at least one setId:familyId"))
          .build();
    }
    // FR-012 / FR-011: every family must be known (an unconfigured repo yields an empty catalog ->
    // all unknown)
    for (String family : request.families()) {
      String[] parts = family == null ? new String[0] : family.split(":", 2);
      if (parts.length != 2
          || parts[0].isBlank()
          || parts[1].isBlank()
          || !catalogService.isKnownFamily(parts[0], parts[1])) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse("unknown family: " + family))
            .build();
      }
    }
    // Spec 009 (US2): every selected agent id must exist -> 400, no half-configured atelier
    try {
      new AgentConfigResolver(agentStore).resolve(request.agentIds(), new java.util.Random());
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ErrorResponse("invalid agent selection: " + e.getMessage()))
          .build();
    }
    // Spec 007 (FR-007, API v0.7.0): unknown inheritFactsFrom -> 400
    if (request.inheritFactsFrom() != null
        && !request.inheritFactsFrom().isBlank()
        && service.get(request.inheritFactsFrom()).isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ErrorResponse("unknown inheritFactsFrom: " + request.inheritFactsFrom()))
          .build();
    }
    // Spec 009 (FR-013): a missing apiKeyVar env var surfaces here as 400 naming the variable
    try {
      AtelierResponse response = service.createAndRun(request);
      return Response.status(Response.Status.CREATED).entity(response).build();
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ErrorResponse(e.getMessage()))
          .build();
    }
  }

  @GET
  public List<AtelierSummary> list() {
    return service.list();
  }

  @GET
  @Path("{id}")
  public Response get(@PathParam("id") String id) {
    return service
        .get(id)
        .map(body -> Response.ok(body).build())
        .orElseGet(() ->
            Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Unknown atelier id: " + id))
                .build());
  }
}
