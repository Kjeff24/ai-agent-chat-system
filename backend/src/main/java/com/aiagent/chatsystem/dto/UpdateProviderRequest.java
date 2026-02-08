package com.aiagent.chatsystem.dto;

import java.util.List;

/**
 * Request body for PATCH /api/models/registry/{provider}.
 * Only dynamic providers can be updated. All fields are optional; omitted fields keep existing values.
 */
public class UpdateProviderRequest {

    /** New API key (for openai/anthropic). For bedrock: AWS access key. Omit to keep current. */
    private String apiKey;

    /** New AWS secret key (for bedrock only). Omit to keep current. */
    private String secretKey;

    /** New base URL. For bedrock: AWS region (e.g. us-east-1). Omit to keep current. */
    private String baseUrl;

    /** New list of model names. Omit to keep current. */
    private List<String> models;

    /** New default model (must be in models if models is provided). Omit to keep current. */
    private String defaultModel;

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
