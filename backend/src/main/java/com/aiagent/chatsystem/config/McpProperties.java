package com.aiagent.chatsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration for MCP (Model Context Protocol) client.
 * Binds to {@code mcp.servers} in application.yml.
 * Tools are discovered from each server at runtime via listTools(); no tool list or input schema is configured here.
 */
@Component
@ConfigurationProperties(prefix = "mcp")
public class McpProperties {

    private boolean enabled = false;

    /**
     * Map of server name -> server config (url, request-timeout-seconds, headers).
     */
    private Map<String, McpServerConfig> servers = Collections.emptyMap();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, McpServerConfig> getServers() {
        return servers;
    }

    public void setServers(Map<String, McpServerConfig> servers) {
        this.servers = servers != null ? servers : Collections.emptyMap();
    }

    public static class McpServerConfig {
        private String url;
        private int requestTimeoutSeconds = 30;
        /** Optional HTTP headers (e.g. Authorization: Bearer &lt;token&gt; for GitHub MCP). Ignored when oauthProvider is set. */
        private Map<String, String> headers = Collections.emptyMap();
        /** Optional OAuth provider id (e.g. "atlassian"). When set, auth uses per-user OAuth tokens instead of static headers. */
        private String oauthProvider;

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
            this.headers = headers != null ? new LinkedHashMap<>(headers) : Collections.emptyMap();
        }

        public String getOauthProvider() {
            return oauthProvider != null ? oauthProvider : "";
        }

        public void setOauthProvider(String oauthProvider) {
            this.oauthProvider = oauthProvider != null ? oauthProvider : "";
        }
    }
}
