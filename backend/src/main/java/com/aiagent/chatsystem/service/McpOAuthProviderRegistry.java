package com.aiagent.chatsystem.service;

import com.aiagent.chatsystem.config.AppMcpOAuthProperties;

import java.util.Map;

/**
 * Registry of OAuth providers for MCP: merges static (from application.yml) with dynamic (added via API).
 * Used by {@link McpOAuthService} to resolve provider config by id.
 */
public interface McpOAuthProviderRegistry {

    /**
     * All effective providers (static + dynamic). Dynamic overrides static if same id.
     */
    Map<String, AppMcpOAuthProperties.OAuthProviderConfig> getEffectiveProviders();

    /**
     * Get provider config by id, or null if not found. Prefers dynamic over static.
     */
    AppMcpOAuthProperties.OAuthProviderConfig getProvider(String providerId);

    /**
     * Source of the provider: "static" (from yaml) or "dynamic" (added via API).
     */
    String getSource(String providerId);

    /**
     * Add or overwrite a dynamic provider.
     */
    void addProvider(String providerId, AppMcpOAuthProperties.OAuthProviderConfig config);

    /**
     * Update a dynamic provider. No-op if provider is static.
     */
    void updateProvider(String providerId, AppMcpOAuthProperties.OAuthProviderConfig config);

    /**
     * Remove a dynamic provider. Returns false if provider is static or not found.
     */
    boolean removeProvider(String providerId);
}
