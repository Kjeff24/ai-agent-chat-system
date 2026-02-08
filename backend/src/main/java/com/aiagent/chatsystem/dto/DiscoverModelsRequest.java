package com.aiagent.chatsystem.dto;

/**
 * Request body for POST /api/models/registry/discover.
 * Supplies provider type and optional credentials to list available models.
 */
public class DiscoverModelsRequest {

    /** Provider type: openai, anthropic, ollama, or bedrock. */
    private String type;

    /** For openai/anthropic: API key. For bedrock: AWS access key. */
    private String apiKey;

    /** For bedrock: AWS secret key. */
    private String secretKey;

    /** Base URL (openai/ollama). For bedrock: AWS region (e.g. us-east-1). */
    private String baseUrl;

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
}
