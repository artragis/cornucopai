package com.cornucopai.api;

import com.cornucopai.api.dto.AtelierResponse;
import com.cornucopai.api.dto.AtelierSummary;
import com.cornucopai.api.dto.CreateAtelierRequest;
import com.cornucopai.api.dto.ErrorResponse;
import com.cornucopai.game.AtelierService;
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
