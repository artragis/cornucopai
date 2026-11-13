package com.cornucopai.api;

import com.cornucopai.api.dto.AgentDto;
import com.cornucopai.api.dto.ErrorResponse;
import com.cornucopai.game.AgentNameConflictException;
import com.cornucopai.game.AgentNotFoundException;
import com.cornucopai.game.AgentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.util.List;

/**
 * REST contract {@code specs/009-configurable-agents/contracts/agents-api.yaml} v1.0.0 (spec 009
 * US-1, FR-001…FR-005): CRUD for configurable LLM agents. 400 = invalid body (FR-019), 404 =
 * unknown id, 409 = duplicate agentName (FR-018).
 */
@Path("/api/agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AgentResource {

  @Inject AgentService service;

  @GET
  public List<AgentDto> list() {
    return service.list().stream().map(AgentDto::of).toList();
  }

  @POST
  public Response create(AgentDto.AgentInput input) {
    if (input == null) {
      return badRequest("request body is required");
    }
    try {
      var created =
          service.create(
              input.agentName(),
              input.openAiCompatibleUrl(),
              input.apiKeyVar(),
              input.modelName(),
              input.personalityPrompt());
      return Response.created(UriBuilder.fromPath("/api/agents/{id}").build(created.id()))
          .entity(AgentDto.of(created))
          .build();
    } catch (IllegalArgumentException e) {
      return badRequest(e.getMessage());
    } catch (AgentNameConflictException e) {
      return Response.status(Response.Status.CONFLICT)
          .entity(new ErrorResponse(e.getMessage()))
          .build();
    }
  }

  @GET
  @Path("{id}")
  public Response get(@PathParam("id") String id) {
    try {
      return Response.ok(AgentDto.of(service.get(id))).build();
    } catch (AgentNotFoundException e) {
      return notFound(e.getMessage());
    }
  }

  @PUT
  @Path("{id}")
  public Response update(@PathParam("id") String id, AgentDto.AgentInput input) {
    if (input == null) {
      return badRequest("request body is required");
    }
    try {
      var updated =
          service.update(
              id,
              input.agentName(),
              input.openAiCompatibleUrl(),
              input.apiKeyVar(),
              input.modelName(),
              input.personalityPrompt());
      return Response.ok(AgentDto.of(updated)).build();
    } catch (AgentNotFoundException e) {
      return notFound(e.getMessage());
    } catch (IllegalArgumentException e) {
      return badRequest(e.getMessage());
    } catch (AgentNameConflictException e) {
      return Response.status(Response.Status.CONFLICT)
          .entity(new ErrorResponse(e.getMessage()))
          .build();
    }
  }

  @DELETE
  @Path("{id}")
  public Response delete(@PathParam("id") String id) {
    try {
      service.delete(id);
      return Response.noContent().build();
    } catch (AgentNotFoundException e) {
      return notFound(e.getMessage());
    }
  }

  private static Response badRequest(String message) {
    return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(message)).build();
  }

  private static Response notFound(String message) {
    return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(message)).build();
  }
}
