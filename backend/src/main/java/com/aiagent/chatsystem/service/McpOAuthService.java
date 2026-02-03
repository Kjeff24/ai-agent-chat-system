package com.aiagent.chatsystem.service;

import java.util.UUID;

/**
 * OAuth flow for MCP servers that use OAuth (e.g. Atlassian).
 * Builds authorize URL, exchanges code for tokens, stores and retrieves per-user tokens.
 */
public interface McpOAuthService {

    /**
     * Build the redirect URL to send the user to the OAuth provider's authorization page.
     * State is a signed JWT containing userId, serverName, and providerId.
     *
     * @param serverName   MCP server name (stored in state for callback)
     * @param userId       current user id
     * @param providerId   OAuth provider id from app.mcp.oauth.providers (e.g. "atlassian")
     * @return redirect URL, or null if provider config is missing
     */
    String buildAuthorizeUrl(String serverName, UUID userId, String providerId);

    /**
     * Exchange the authorization code for tokens, validate state, and store the token for the user/server.
     * State JWT contains userId, serverName, providerId.
     *
     * @param code   authorization code from callback
     * @param state  state from callback (signed JWT with userId, serverName, providerId)
     * @return the userId that was in state (for redirect), or null if state invalid or exchange failed
     */
    UUID exchangeCodeAndStoreToken(String code, String state);

    /**
     * Get a valid access token for the user and server (refreshes if expired and refresh_token is available).
     *
     * @param userId     user id
     * @param serverName MCP server name
     * @return access token, or null if no token or expired and cannot refresh
     */
    String getValidAccessToken(UUID userId, String serverName);

    /**
     * Whether the user has a stored token for this server (may be expired).
     */
    boolean hasToken(UUID userId, String serverName);

    /**
     * Remove stored token for user/server (e.g. "disconnect").
     */
    void revokeToken(UUID userId, String serverName);
}
