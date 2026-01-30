package com.aiagent.chatsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configuration for MCP (Model Context Protocol) client.
 * Binds to {@code mcp.servers} in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "mcp")
public class McpProperties {

    private boolean enabled = false;

    /**
     * Fallback: MCP tool(s) when a server does not define its own default-context-tool.
     * Can be a list in YAML or comma-separated in env (e.g. MCP_DEFAULT_CONTEXT_TOOL=search_repositories).
     */
    private List<String> defaultContextTools = Collections.emptyList();

    /**
     * Map of server name -> server config (url, request-timeout-seconds).
     */
    private Map<String, McpServerConfig> servers = Collections.emptyMap();

    /**
     * Fallback: per-tool query transforms when a server does not define its own query-transforms.
     * Key = tool name; value = list of { pattern, template }. $1, $2 in template = regex capture groups.
     */
    private Map<String, List<QueryTransformRule>> queryTransforms = Collections.emptyMap();

    public List<String> getDefaultContextTools() {
        return defaultContextTools == null ? Collections.emptyList() : defaultContextTools;
    }

    /** Bind from YAML list (default-context-tool: [search_repositories, list_repositories]). */
    public void setDefaultContextTool(List<String> defaultContextTools) {
        this.defaultContextTools = defaultContextTools == null ? Collections.emptyList()
                : defaultContextTools.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).toList();
    }

    /** Bind from env single value; comma-separated names are split (e.g. search_repositories,list_repositories). */
    public void setDefaultContextTool(String value) {
        if (value == null || value.isBlank()) {
            this.defaultContextTools = Collections.emptyList();
            return;
        }
        this.defaultContextTools = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

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

    public Map<String, List<QueryTransformRule>> getQueryTransforms() {
        return queryTransforms == null ? Collections.emptyMap() : queryTransforms;
    }

    public void setQueryTransforms(Map<String, List<QueryTransformRule>> queryTransforms) {
        this.queryTransforms = queryTransforms != null ? queryTransforms : Collections.emptyMap();
    }

    public static class QueryTransformRule {
        private String pattern;
        private String template;

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }
    }

    public static class McpServerConfig {
        private String url;
        private int requestTimeoutSeconds = 30;
        /** Optional HTTP headers (e.g. Authorization: Bearer &lt;token&gt; for GitHub MCP). */
        private Map<String, String> headers = Collections.emptyMap();
        /**
         * Tool(s) to call for this server with the last user message as "query"; results are combined as context.
         * Overrides top-level mcp.default-context-tool when set. Single value, list, or comma-separated string.
         */
        private List<String> defaultContextTools = Collections.emptyList();
        /**
         * Per-tool query transforms for this server. Overrides top-level mcp.query-transforms when set.
         * Key = tool name; value = list of { pattern, template }.
         */
        private Map<String, List<QueryTransformRule>> queryTransforms = Collections.emptyMap();

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

        public List<String> getDefaultContextTools() {
            return defaultContextTools == null ? Collections.emptyList() : defaultContextTools;
        }

        public void setDefaultContextTools(List<String> defaultContextTools) {
            this.defaultContextTools = defaultContextTools == null ? Collections.emptyList()
                    : defaultContextTools.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).toList();
        }

        /** Bind single value or comma-separated (e.g. default-context-tool: search_repositories). */
        public void setDefaultContextTool(String value) {
            if (value == null || value.isBlank()) {
                this.defaultContextTools = Collections.emptyList();
                return;
            }
            this.defaultContextTools = Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
        }

        public Map<String, List<QueryTransformRule>> getQueryTransforms() {
            return queryTransforms == null ? Collections.emptyMap() : queryTransforms;
        }

        public void setQueryTransforms(Map<String, List<QueryTransformRule>> queryTransforms) {
            this.queryTransforms = queryTransforms != null ? queryTransforms : Collections.emptyMap();
        }
    }
}
