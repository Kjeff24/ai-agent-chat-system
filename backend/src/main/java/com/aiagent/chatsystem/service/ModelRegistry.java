package com.aiagent.chatsystem.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for dynamically managing AI chat models.
 * Models can be registered at startup (static) or at runtime via API (dynamic).
 * Only dynamically registered providers can be unregistered via API.
 * Dynamic providers can store metadata (list of models, default model) for UI/model selection.
 */
@Component
public class ModelRegistry {

    private final Map<String, ChatModel> models = new ConcurrentHashMap<>();
    private final Set<String> dynamicProviders = ConcurrentHashMap.newKeySet();
    private final Map<String, ProviderMetadata> metadataByProvider = new ConcurrentHashMap<>();

    /**
     * Register a chat model (e.g. at startup). Not tracked as dynamic; no metadata.
     */
    public void registerModel(String provider, ChatModel model) {
        models.put(provider.toLowerCase(), model);
    }

    /**
     * Register a chat model at runtime via API. Can be unregistered via {@link #unregisterModel(String)}.
     * Metadata (models list, default model) is optional and used for listing available models per provider.
     */
    public void registerDynamicModel(String provider, ChatModel model, ProviderMetadata metadata) {
        String key = provider.toLowerCase();
        models.put(key, model);
        dynamicProviders.add(key);
        if (metadata != null) {
            metadataByProvider.put(key, metadata);
        } else {
            metadataByProvider.remove(key);
        }
    }

    /** @deprecated Use {@link #registerDynamicModel(String, ChatModel, ProviderMetadata)} */
    @Deprecated
    public void registerDynamicModel(String provider, ChatModel model) {
        registerDynamicModel(provider, model, null);
    }

    /**
     * Unregister a provider. Only succeeds if the provider was registered dynamically via API.
     * Returns true if removed, false if not found or not dynamic.
     */
    public boolean unregisterModel(String provider) {
        String key = provider.toLowerCase();
        if (!dynamicProviders.contains(key)) return false;
        dynamicProviders.remove(key);
        models.remove(key);
        metadataByProvider.remove(key);
        return true;
    }

    /**
     * Get metadata for a provider (list of models, default model). Only present for dynamically
     * registered providers that were registered with metadata.
     */
    public ProviderMetadata getProviderMetadata(String provider) {
        return provider == null ? null : metadataByProvider.get(provider.toLowerCase());
    }

    /**
     * Whether the provider was registered dynamically (via API).
     */
    public boolean isDynamic(String provider) {
        return dynamicProviders.contains(provider != null ? provider.toLowerCase() : null);
    }

    public ChatModel getModel(String provider) {
        return models.get(provider != null ? provider.toLowerCase() : null);
    }

    public boolean hasModel(String provider) {
        return models.containsKey(provider != null ? provider.toLowerCase() : null);
    }

    public Set<String> getRegisteredProviders() {
        return models.keySet();
    }

    /**
     * First registered provider key, or null if none. Used as default for new conversations.
     */
    public String getDefaultProviderKey() {
        return models.isEmpty() ? null : models.keySet().iterator().next();
    }

    /**
     * Default model for a provider (from metadata), or null if unknown.
     */
    public String getDefaultModel(String providerKey) {
        ProviderMetadata meta = getProviderMetadata(providerKey);
        if (meta == null) return null;
        if (meta.defaultModel() != null && !meta.defaultModel().isBlank()) return meta.defaultModel();
        return meta.models().isEmpty() ? null : meta.models().get(0);
    }
}
