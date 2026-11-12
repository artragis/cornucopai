package com.cornucopai.game.llm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Server-wide LLM defaults (spec 004, R-028, FR-002): read from {@code cornucopai.llm.*} properties
 * (env-overridable). The default base URL is the local bionic server {@code
 * http://localhost:1235/v1} with NO authentication, so a game launches with zero configuration and
 * zero external cost.
 */
@ApplicationScoped
public class LlmDefaults {

  private final String baseUrl;
  private final String apiKey;
  private final String model;
  private final Duration timeout;

  @Inject
  public LlmDefaults(
      @ConfigProperty(name = "cornucopai.llm.base-url", defaultValue = "http://localhost:1235/v1")
          String baseUrl,
      // Optional: an empty ${LLM_API_KEY:} is "defined as empty" (treated as null by SmallRye)
      @ConfigProperty(name = "cornucopai.llm.api-key") Optional<String> apiKey,
      @ConfigProperty(name = "cornucopai.llm.model") Optional<String> model,
      @ConfigProperty(name = "cornucopai.llm.timeout-seconds", defaultValue = "120")
          long timeoutSeconds) {
    this(baseUrl, apiKey.orElse(null), model.orElse(null), Duration.ofSeconds(timeoutSeconds));
  }

  /** Test-friendly constructor. */
  public LlmDefaults(String baseUrl, String apiKey, String model, Duration timeout) {
    this.baseUrl = baseUrl;
    this.apiKey = blankToNull(apiKey);
    this.model = blankToNull(model);
    this.timeout = timeout;
  }

  /** The default per-agent config: OPENAI_COMPATIBLE on the default base URL, no key. */
  public AgentLlmConfig defaultConfig() {
    return new AgentLlmConfig(
        AgentLlmConfig.ProviderType.OPENAI_COMPATIBLE, baseUrl, apiKey, model, null, null);
  }

  public String baseUrl() {
    return baseUrl;
  }

  public String apiKey() {
    return apiKey;
  }

  public String model() {
    return model;
  }

  public Duration timeout() {
    return timeout;
  }

  public static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s;
  }
}
