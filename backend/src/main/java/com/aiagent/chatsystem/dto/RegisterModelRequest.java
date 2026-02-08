package com.aiagent.chatsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Request body for dynamically registering an AI model provider (OpenAI-compatible, Anthropic, Ollama, or AWS Bedrock).
 * You can supply a list of models and choose which one is the default; the default is used when building
 * the provider and when creating model configs without an explicit model.
 */
public class RegisterModelRequest {

    /** Unique provider key (e.g. openrouter, my-ollama, bedrock). Must not conflict with static providers if you need to override. */
    @NotBlank(message = "Provider name is required")
    private String provider;

    /** Type of provider: openai (OpenAI API, OpenRouter, etc.), anthropic, ollama, or bedrock. */
    @NotBlank(message = "Type is required")
    @Pattern(regexp = "openai|anthropic|ollama|bedrock", message = "Type must be 'openai', 'anthropic', 'ollama', or 'bedrock'")
    private String type;

    /** API key (required for type=openai and type=anthropic). For bedrock: AWS access key (optional if using default credential chain). */
    private String apiKey;

    /** AWS secret key for type=bedrock. Optional if using default credential chain (env vars, ~/.aws/credentials). */
    private String secretKey;

    /**
     * Base URL for the provider API (without the path that the client appends).
     * Examples: https://api.openai.com (OpenAI), https://openrouter.ai/api (OpenRouter), http://localhost:11434 (Ollama).
     * For type=bedrock: use this field as the AWS region (e.g. us-east-1). Leave blank for us-east-1.
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

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
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
