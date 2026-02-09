package com.aiagent.chatsystem.dto;

import java.util.Map;

/**
 * Full details of an MCP server (for GET by name / edit). Includes url, source, requestTimeoutSeconds, headers.
 * Tools are discovered from the server at runtime; no tool list or input schema is stored.
 */
public class McpServerDetailDTO {

    private String name;
    private String url;
    private String source; // "static" | "dynamic"
    /** Connection status: "unknown" (not yet probed), "connected", "failed" */
    private String status;
    /** OAuth provider id when server uses OAuth (e.g. "atlassian"). */
    private String oauthProvider;
    private int requestTimeoutSeconds = 30;
    private Map<String, String> headers;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOauthProvider() {
        return oauthProvider;
    }

    public void setOauthProvider(String oauthProvider) {
        this.oauthProvider = oauthProvider;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}
