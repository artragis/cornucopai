package com.cornucopai.api.dto;

import com.cornucopai.game.llm.AgentLlmConfig;
import com.cornucopai.game.llm.LlmDefaults;
import com.cornucopai.game.model.Archetype;

/**
 * One entry of the optional {@code agents} array of {@code CreateAtelierRequest}
 * (contracts/ateliers-api.yaml v0.4.0, R-032). {@code name}/{@code archetype} default
 * to {@code Joueur-N} / round-robin; the LLM provider fields default to the server
 * {@link LlmDefaults} (FR-002).
 */
public record AgentConfigDto(String name, Archetype archetype, String providerType,
                             String baseUrl, String apiKey, String modelName,
                             String acpProvider, String command) {

    /**
     * Resolves this DTO into a validated {@link AgentLlmConfig}. A {@code null}
     * baseUrl falls back to the server default; a present-but-blank baseUrl is
     * rejected (edge case: invalid config → 400, no half-configured atelier).
     */
    public AgentLlmConfig toConfig(LlmDefaults defaults) {
        AgentLlmConfig.ProviderType type = AgentLlmConfig.parseProviderType(providerType);
        String resolvedBaseUrl = null;
        if (type == AgentLlmConfig.ProviderType.OPENAI_COMPATIBLE) {
            if (baseUrl == null) {
                resolvedBaseUrl = defaults.baseUrl();
            } else if (baseUrl.isBlank()) {
                throw new IllegalArgumentException("baseUrl must not be blank when providerType=OPENAI_COMPATIBLE (omit it to use the server default)");
            } else {
                resolvedBaseUrl = baseUrl;
            }
        }
        String resolvedKey = LlmDefaults.blankToNull(apiKey) != null ? apiKey : defaults.apiKey();
        String resolvedModel = LlmDefaults.blankToNull(modelName) != null ? modelName : defaults.model();
        return new AgentLlmConfig(type, resolvedBaseUrl, resolvedKey, resolvedModel, acpProvider, command);
    }
}
