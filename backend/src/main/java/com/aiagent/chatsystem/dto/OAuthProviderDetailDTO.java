package com.aiagent.chatsystem.dto;

/**
 * Detail of an OAuth provider for edit. Does not include clientSecret.
 */
public class OAuthProviderDetailDTO {

    private String id;
    private String authorizationUri;
    private String tokenUri;
    private String clientId;
    private String scopes;
    /** "static" (from yaml) or "dynamic" (added via API). */
    private String source;

    public OAuthProviderDetailDTO() {
    }

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

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
