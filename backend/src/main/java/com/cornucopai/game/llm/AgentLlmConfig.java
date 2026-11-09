package com.cornucopai.game.llm;

import java.util.Set;

/**
 * Resolved LLM provider configuration of one player agent (spec 004, research R-028).
 * Persisted in {@code atelier.json} as {@code agentConfigs[]}, index-aligned with
 * {@code players} (FR-012); see contracts/atelier-schema.json.
 *
 * <p>Two provider kinds (FR-001): {@link ProviderType#OPENAI_COMPATIBLE} — an
 * OpenAI-compatible base URL (+ optional apiKey/modelName); {@link ProviderType#ACP} —
 * an ACP agent such as {@code github-copilot} or {@code mistral-vibe}.
 */
public record AgentLlmConfig(ProviderType providerType, String baseUrl, String apiKey,
                             String modelName, String acpProvider, String command) {

    public enum ProviderType { OPENAI_COMPATIBLE, ACP }

    /** Known ACP providers (FR-001). */
    public static final Set<String> ACP_PROVIDERS = Set.of("github-copilot", "mistral-vibe");

    public AgentLlmConfig {
        validate(providerType, baseUrl, acpProvider);
    }

    /** Validation rules from data-model.md — unknown/invalid configs are rejected (400 at launch). */
    public static void validate(ProviderType providerType, String baseUrl, String acpProvider) {
        if (providerType == null) {
            throw new IllegalArgumentException("providerType is required");
        }
        switch (providerType) {
            case OPENAI_COMPATIBLE -> {
                if (baseUrl == null || baseUrl.isBlank()) {
                    throw new IllegalArgumentException("baseUrl is required when providerType=OPENAI_COMPATIBLE");
                }
            }
            case ACP -> {
                if (acpProvider == null || !ACP_PROVIDERS.contains(acpProvider)) {
                    throw new IllegalArgumentException("acpProvider must be one of " + ACP_PROVIDERS + " when providerType=ACP");
                }
            }
        }
    }

    /** The default provider: local OpenAI-compatible server, no authentication (FR-002). */
    public static AgentLlmConfig defaultOpenAiCompatible(String baseUrl) {
        return new AgentLlmConfig(ProviderType.OPENAI_COMPATIBLE, baseUrl, null, null, null, null);
    }

    /** Case-insensitive parse used by the API layer; unknown values are rejected. */
    public static ProviderType parseProviderType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("providerType is required");
        }
        try {
            return ProviderType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown providerType: " + value);
        }
    }
}
