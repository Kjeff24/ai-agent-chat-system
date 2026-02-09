package com.aiagent.chatsystem.service;

import com.aiagent.chatsystem.dto.RegisterModelRequest;
import com.aiagent.chatsystem.dto.UpdateProviderRequest;

import java.util.Map;

/**
 * Service for managing dynamically registered AI model providers:
 * list providers, get provider details, register, update, and unregister.
 * Business logic lives here; controllers delegate to this service.
 */
public interface ModelProviderManagementService {

    /**
     * Get all registered providers with metadata (name, dynamic flag, models, defaultModel).
     * @return map with keys: providers (Set), count (int), providersWithMeta (List of maps)
     */
    Map<String, Object> getRegisteredProvidersSummary();

    /**
     * Get details for a single provider (registered, dynamic, type, baseUrl, models, defaultModel, apiKeyMasked).
     * @param provider provider key (e.g. openai, ollama)
     * @return map with provider, registered, dynamic, and when dynamic: type, baseUrl, models, defaultModel, apiKeyMasked
     */
    Map<String, Object> getProviderDetails(String provider);

    /**
     * Register a new provider at runtime and persist it.
     * @param request registration request (provider, type, apiKey, baseUrl, models, defaultModel)
     * @return map with provider, status ("registered"), defaultModel
     */
    Map<String, String> registerProvider(RegisterModelRequest request);

    /**
     * Update a dynamically registered provider. Only dynamic providers can be updated.
     * @param provider provider key to update
     * @param request optional fields to update (apiKey, baseUrl, models, defaultModel)
     * @return map with provider, status ("updated"), defaultModel
     */
    Map<String, String> updateProvider(String provider, UpdateProviderRequest request);

    /**
     * Unregister a dynamically registered provider. Fails if not dynamic.
     * @param provider provider key to remove
     */
    void unregisterProvider(String provider);
}
