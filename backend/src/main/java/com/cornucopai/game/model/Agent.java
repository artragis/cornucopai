package com.cornucopai.game.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.URI;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A configurable LLM agent (spec 009): persisted as one JSON file {@code agents/{id}.json}
 * (research R-001, contracts/agent-schema.json). An agent is always LLM-backed; the old {@code
 * Archetype} enum is replaced by the free-text {@code personalityPrompt} (FR-012).
 *
 * <p>{@code apiKeyVar} stores only the NAME of the environment variable holding the API key; the
 * key value is resolved server-side at game launch via {@code System.getenv} and is never persisted
 * (FR-006/FR-013/FR-017).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Agent(
    String id,
    String agentName,
    String openAiCompatibleUrl,
    String apiKeyVar,
    String modelName,
    String personalityPrompt) {

  /** FR-019: empty (no key required) or a valid POSIX env var name. */
  public static final Pattern ENV_VAR_NAME = Pattern.compile("^$|^[A-Za-z_][A-Za-z0-9_]*$");

  public Agent {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("id is required");
    }
    try {
      UUID.fromString(id.trim());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("id must be a UUID: " + id);
    }
    if (agentName == null || agentName.isBlank()) {
      throw new IllegalArgumentException("agentName is required and must be non-blank");
    }
    if (!isHttpUrl(openAiCompatibleUrl)) {
      throw new IllegalArgumentException(
          "openAiCompatibleUrl must be an absolute http/https URL: " + openAiCompatibleUrl);
    }
    if (apiKeyVar == null) {
      apiKeyVar = "";
    }
    if (!ENV_VAR_NAME.matcher(apiKeyVar).matches()) {
      throw new IllegalArgumentException(
          "apiKeyVar must be empty or a valid environment variable name: " + apiKeyVar);
    }
    if (modelName == null || modelName.isBlank()) {
      throw new IllegalArgumentException("modelName is required and must be non-blank");
    }
    if (personalityPrompt == null || personalityPrompt.isBlank()) {
      throw new IllegalArgumentException("personalityPrompt is required and must be non-blank");
    }
  }

  /** Absolute http/https URL check (FR-019, research R-003). */
  public static boolean isHttpUrl(String url) {
    if (url == null || url.isBlank()) {
      return false;
    }
    try {
      URI uri = new URI(url.trim());
      String scheme = uri.getScheme();
      return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
          && uri.getHost() != null;
    } catch (Exception e) {
      return false;
    }
  }

  /** Creates a new agent with a server-generated UUID id (FR-003). */
  public static Agent create(
      String agentName,
      String openAiCompatibleUrl,
      String apiKeyVar,
      String modelName,
      String personalityPrompt) {
    return new Agent(
        UUID.randomUUID().toString(),
        agentName,
        openAiCompatibleUrl,
        apiKeyVar,
        modelName,
        personalityPrompt);
  }

  /** The launch snapshot copied into the atelier (FR-017) — never contains a key value. */
  public com.cornucopai.game.llm.AgentSnapshot toSnapshot() {
    return new com.cornucopai.game.llm.AgentSnapshot(
        id, agentName, openAiCompatibleUrl, apiKeyVar, modelName, personalityPrompt);
  }
}
