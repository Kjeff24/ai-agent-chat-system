package com.aiagent.chatsystem.dto;

/**
 * Request body for PUT /api/models/registry/default-provider.
 */
public class DefaultProviderRequest {
    private String providerKey;

    public String getProviderKey() {
        return providerKey;
    }

    public void setProviderKey(String providerKey) {
        this.providerKey = providerKey;
    }
}
