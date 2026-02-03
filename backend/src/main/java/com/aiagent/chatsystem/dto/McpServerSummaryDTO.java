package com.aiagent.chatsystem.dto;

/**
 * Summary of an MCP server (name, url, source, connection status).
 */
public class McpServerSummaryDTO {

    private String name;
    private String url;
    private String source; // "static" | "dynamic"
    /** Connection status: "unknown" (not yet probed), "connected", "failed" */
    private String status;
    /** OAuth provider id when server uses OAuth (e.g. "atlassian"). */
    private String oauthProvider;

    public McpServerSummaryDTO() {
    }

    public McpServerSummaryDTO(String name, String url, String source) {
        this.name = name;
        this.url = url;
        this.source = source;
    }

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
}
