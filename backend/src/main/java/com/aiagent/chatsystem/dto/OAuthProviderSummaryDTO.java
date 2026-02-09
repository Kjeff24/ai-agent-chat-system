package com.aiagent.chatsystem.dto;

/**
 * Summary of an OAuth provider (id, endpoints, masked client id, source).
 */
public class OAuthProviderSummaryDTO {

    private String id;
    private String authorizationUri;
    private String tokenUri;
    private String clientIdMasked;
    private String scopes;
    /** "static" (from yaml) or "dynamic" (added via API). */
    private String source;

    public OAuthProviderSummaryDTO() {
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

    public String getClientIdMasked() {
        return clientIdMasked;
    }

    public void setClientIdMasked(String clientIdMasked) {
        this.clientIdMasked = clientIdMasked;
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
