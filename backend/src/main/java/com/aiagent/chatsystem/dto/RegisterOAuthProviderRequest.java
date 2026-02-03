package com.aiagent.chatsystem.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to add an OAuth provider (dynamic).
 */
public class RegisterOAuthProviderRequest {

    @NotBlank(message = "Provider id is required")
    private String id;

    @NotBlank(message = "Authorization URI is required")
    private String authorizationUri;

    @NotBlank(message = "Token URI is required")
    private String tokenUri;

    private String clientId;
    private String clientSecret;
    private String scopes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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
