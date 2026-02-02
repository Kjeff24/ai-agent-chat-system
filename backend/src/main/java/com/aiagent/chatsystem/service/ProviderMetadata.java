package com.aiagent.chatsystem.service;

import java.util.Collections;
import java.util.List;

/**
 * Metadata for a registered provider: list of available models and the default model.
 * Used so clients can show model pickers and know which model is the default.
 */
public record ProviderMetadata(
        List<String> models,
        String defaultModel
) {
    public ProviderMetadata {
        models = models != null ? List.copyOf(models) : Collections.emptyList();
    }

    public static ProviderMetadata of(List<String> models, String defaultModel) {
        return new ProviderMetadata(models, defaultModel);
    }
}
