package com.cornucopai.api;

import com.cornucopai.api.dto.AtelierResponse;
import com.cornucopai.api.dto.AtelierSummary;
import com.cornucopai.api.dto.CreateAtelierRequest;
import com.cornucopai.api.dto.ErrorResponse;
import com.cornucopai.game.AtelierService;
import com.cornucopai.game.card.CardCatalogService;
import com.cornucopai.game.llm.AgentConfigResolver;
import com.cornucopai.game.llm.LlmDefaults;
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

/**
 * REST contract: specs/002-cornucopia-game-flow/contracts/ateliers-api.yaml.
 * POST runs the whole game synchronously (research R-003).
 */
@Path("/api/ateliers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AtelierResource {

    @Inject
    AtelierService service;

    @Inject
    CardCatalogService catalogService;

    @Inject
    LlmDefaults llmDefaults;

    @POST
    public Response create(CreateAtelierRequest request) {
        if (request == null || request.playerCount() == null || request.playerCount() < 2) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("playerCount must be >= 2"))
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
        // FR-012 / FR-011: every family must be known (an unconfigured repo yields an empty catalog -> all unknown)
        for (String family : request.families()) {
            String[] parts = family == null ? new String[0] : family.split(":", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()
                    || !catalogService.isKnownFamily(parts[0], parts[1])) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("unknown family: " + family))
                        .build();
            }
        }
        // Spec 004: validate the optional per-agent LLM configs (unknown providerType/acpProvider,
        // blank baseUrl, agents.size() > playerCount) -> 400, no half-configured atelier (R-032)
        try {
            new AgentConfigResolver(llmDefaults).resolve(request.playerCount(), request.agents());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("invalid agent config: " + e.getMessage()))
                    .build();
        }
        AtelierResponse response = service.createAndRun(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    public List<AtelierSummary> list() {
        return service.list();
    }

    @GET
    @Path("{id}")
    public Response get(@PathParam("id") String id) {
        return service.get(id)
                .map(body -> Response.ok(body).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Unknown atelier id: " + id))
                        .build());
    }
}
