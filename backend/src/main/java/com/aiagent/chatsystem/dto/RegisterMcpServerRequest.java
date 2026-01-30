package com.aiagent.chatsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.Map;

/**
 * Request body for dynamically registering an MCP server (Streamable HTTP).
 */
public class RegisterMcpServerRequest {

    @NotBlank(message = "Server name is required")
    private String name;

    @NotBlank(message = "Server URL is required")
    private String url;

    @Positive
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
