package com.aiagent.chatsystem.service;

import java.util.Collections;
import java.util.List;

/**
 * Metadata for a registered provider: list of available models, the default model, and optional type (openai, ollama, etc.).
 * Used so clients can show model pickers and the backend can resolve default provider/model and build request options.
 */
public record ProviderMetadata(
        List<String> models,
        String defaultModel,
        String type
) {
    public ProviderMetadata {
        models = models != null ? List.copyOf(models) : Collections.emptyList();
    }

    public static ProviderMetadata of(List<String> models, String defaultModel) {
        return new ProviderMetadata(models, defaultModel, null);
    }

    public static ProviderMetadata of(List<String> models, String defaultModel, String type) {
        return new ProviderMetadata(models, defaultModel, type);
    }
}
