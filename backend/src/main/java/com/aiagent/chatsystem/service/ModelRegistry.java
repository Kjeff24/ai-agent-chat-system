package com.aiagent.chatsystem.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for dynamically managing AI chat models.
 * Models can be registered at runtime without requiring all providers to be configured.
 */
@Component
public class ModelRegistry {
    
    private final Map<String, ChatModel> models = new ConcurrentHashMap<>();
    
    /**
     * Register a chat model for a specific provider
     */
    public void registerModel(String provider, ChatModel model) {
        models.put(provider.toLowerCase(), model);
    }
    
    /**
     * Unregister a model
     */
    public void unregisterModel(String provider) {
        models.remove(provider.toLowerCase());
    }
    
    /**
     * Get a model by provider name
     */
    public ChatModel getModel(String provider) {
        return models.get(provider.toLowerCase());
    }
    
    /**
     * Check if a model is registered
     */
    public boolean hasModel(String provider) {
        return models.containsKey(provider.toLowerCase());
    }
    
    /**
     * Get all registered providers
     */
    public java.util.Set<String> getRegisteredProviders() {
        return models.keySet();
    }
}
