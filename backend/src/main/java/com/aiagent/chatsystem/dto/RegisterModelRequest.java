package com.aiagent.chatsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Request body for dynamically registering an AI model provider (OpenAI-compatible, Anthropic, or Ollama).
 * You can supply a list of models and choose which one is the default; the default is used when building
 * the provider and when creating model configs without an explicit model.
 */
public class RegisterModelRequest {

    /** Unique provider key (e.g. openrouter, my-ollama). Must not conflict with static providers if you need to override. */
    @NotBlank(message = "Provider name is required")
    private String provider;

    /** Type of provider: openai (OpenAI API, OpenRouter, etc.), anthropic, or ollama. */
    @NotBlank(message = "Type is required")
    @Pattern(regexp = "openai|anthropic|ollama", message = "Type must be 'openai', 'anthropic', or 'ollama'")
    private String type;

    /** API key (required for type=openai and type=anthropic). */
    private String apiKey;

    /**
     * Base URL for the provider API (without the path that the client appends).
     * This app uses Spring AI's OpenAiApi which appends "/v1/chat/completions" to the base URL.
     * Examples: https://api.openai.com (OpenAI), https://openrouter.ai/api (OpenRouter; client adds /v1/chat/completions → https://openrouter.ai/api/v1/chat/completions),
     * http://localhost:11434 (Ollama).
     * See https://openrouter.ai/docs/quickstart for OpenRouter.
     */
    private String baseUrl;

    /**
     * List of model names available for this provider (e.g. ["gpt-4", "gpt-3.5-turbo"]).
     * Optional; if omitted, only defaultModel (or a type-specific default) is used.
     */
    private List<String> models;

    /**
     * Default model name (e.g. gpt-4, llama2). Must be one of {@link #models} if models is non-empty.
     * If models is set and defaultModel is blank, the first element of models is used as default.
     */
    private String defaultModel;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type == null ? null : type.trim().toLowerCase();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<String> getModels() {
        return models;
    }

    public void setModels(List<String> models) {
        this.models = models;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }
}
