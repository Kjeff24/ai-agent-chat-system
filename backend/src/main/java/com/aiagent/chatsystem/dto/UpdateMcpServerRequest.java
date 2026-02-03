package com.aiagent.chatsystem.dto;

import jakarta.validation.constraints.Positive;

import java.util.Map;

/**
 * Request body for updating a dynamically registered MCP server. All fields optional (partial update).
 */
public class UpdateMcpServerRequest {

    private String url;

    @Positive
    private Integer requestTimeoutSeconds;

    private Map<String, String> headers;

    /** Optional OAuth provider id (e.g. "atlassian"). When set, auth uses per-user OAuth. */
    private String oauthProvider;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(Integer requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getOauthProvider() {
        return oauthProvider;
    }

    public void setOauthProvider(String oauthProvider) {
        this.oauthProvider = oauthProvider;
    }
}
