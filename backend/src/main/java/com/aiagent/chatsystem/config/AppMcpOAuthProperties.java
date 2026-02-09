package com.aiagent.chatsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OAuth configuration for MCP servers that require OAuth (e.g. Atlassian).
 * Binds to {@code app.mcp.oauth} in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "app.mcp.oauth")
public class AppMcpOAuthProperties {

    /**
     * Full URL for the OAuth callback (e.g. https://your-backend/api/mcp/servers/oauth/callback).
     * Must match the redirect URI registered with each OAuth provider.
     */
    private String callbackUri = "";

    /**
     * Map of OAuth provider id -> provider config (authorization URI, token URI, client id/secret, scopes).
     * Server config (static or dynamic) references a provider by this id via {@code oauthProvider}.
     */
    private Map<String, OAuthProviderConfig> providers = Collections.emptyMap();

    public String getCallbackUri() {
        return callbackUri != null ? callbackUri : "";
    }

    public void setCallbackUri(String callbackUri) {
        this.callbackUri = callbackUri != null ? callbackUri : "";
    }

    public Map<String, OAuthProviderConfig> getProviders() {
        return providers != null ? providers : Collections.emptyMap();
    }

    public void setProviders(Map<String, OAuthProviderConfig> providers) {
        this.providers = providers != null ? new LinkedHashMap<>(providers) : Collections.emptyMap();
    }

    public static class OAuthProviderConfig {
        private String authorizationUri;
        private String tokenUri;
        private String clientId;
        private String clientSecret;
        private String scopes;

        public String getAuthorizationUri() {
            return authorizationUri;
        }

        public void setAuthorizationUri(String authorizationUri) {
            this.authorizationUri = authorizationUri;
        }

        public String getTokenUri() {
            return tokenUri;
        }

        public void setTokenUri(String tokenUri) {
            this.tokenUri = tokenUri;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getScopes() {
            return scopes;
        }

        public void setScopes(String scopes) {
            this.scopes = scopes;
        }
    }
}
