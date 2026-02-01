package com.aiagent.chatsystem.service.impl;

import com.aiagent.chatsystem.config.McpProperties;
import com.aiagent.chatsystem.dto.McpServerSummaryDTO;
import com.aiagent.chatsystem.dto.RegisterMcpServerRequest;
import com.aiagent.chatsystem.service.McpClientService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP client service using Streamable HTTP transport.
 * Lazily creates and caches one McpSyncClient per configured server.
 * Supports static config (application.yml) and dynamic registration via API.
 */
@Service
public class McpClientServiceImpl implements McpClientService {

    private static final Logger logger = LoggerFactory.getLogger(McpClientServiceImpl.class);

    private final McpProperties properties;
    private final Map<String, McpSyncClient> clients = new ConcurrentHashMap<>();
    private final Map<String, McpProperties.McpServerConfig> dynamicServers = new ConcurrentHashMap<>();

    public McpClientServiceImpl(McpProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isEnabled() {
        return !getAllServerConfigs().isEmpty();
    }

    @Override
    public List<McpServerSummaryDTO> listServers() {
        List<McpServerSummaryDTO> result = new ArrayList<>();
        Map<String, McpProperties.McpServerConfig> staticServers = properties.getServers();
        if (staticServers != null) {
            for (Map.Entry<String, McpProperties.McpServerConfig> e : staticServers.entrySet()) {
                if (e.getValue() != null && e.getValue().getUrl() != null) {
                    result.add(new McpServerSummaryDTO(e.getKey(), e.getValue().getUrl(), "static"));
                }
            }
        }
        for (Map.Entry<String, McpProperties.McpServerConfig> e : dynamicServers.entrySet()) {
            if (e.getValue() != null && e.getValue().getUrl() != null) {
                result.add(new McpServerSummaryDTO(e.getKey(), e.getValue().getUrl(), "dynamic"));
            }
        }
        return result;
    }

    @Override
    public void addServer(RegisterMcpServerRequest request) {
        McpProperties.McpServerConfig config = new McpProperties.McpServerConfig();
        config.setUrl(request.getUrl().trim());
        config.setRequestTimeoutSeconds(request.getRequestTimeoutSeconds() > 0 ? request.getRequestTimeoutSeconds() : 30);
        if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
            config.setHeaders(new LinkedHashMap<>(request.getHeaders()));
        }
        String name = request.getName().trim();
        dynamicServers.put(name, config);
        clients.remove(name);
        logger.info("MCP server registered dynamically: {} -> {}", name, config.getUrl());
    }

    @Override
    public boolean removeServer(String name) {
        if (name == null || name.isBlank()) return false;
        if (!dynamicServers.containsKey(name)) return false;
        dynamicServers.remove(name);
        McpSyncClient removed = clients.remove(name);
        if (removed != null) {
            try {
                removed.closeGracefully();
            } catch (Exception e) {
                logger.debug("Error closing MCP client for {}: {}", name, e.getMessage());
            }
        }
        logger.info("MCP server removed: {}", name);
        return true;
    }

    @Override
    public List<String> listToolNames(String serverName) {
        McpSyncClient client = getOrCreateClient(serverName);
        if (client == null) return List.of();
        try {
            McpSchema.ListToolsResult result = client.listTools();
            if (result == null || result.tools() == null) return List.of();
            return result.tools().stream()
                    .map(McpSchema.Tool::name)
                    .toList();
        } catch (Exception e) {
            logger.warn("MCP listTools failed for server {}: {}", serverName, e.getMessage());
            return List.of();
        }
    }

    @Override
    public String executeTool(String serverName, String toolName, Map<String, Object> arguments) {
        String result = executeToolOnce(serverName, toolName, arguments);
        if (result != null) return result;
        // Retry once with a fresh client (helps recover from 405/SSE leaving session in bad state)
        logger.info("MCP executeTool retrying with fresh client for server {} tool {}", serverName, toolName);
        invalidateClient(serverName);
        result = executeToolOnce(serverName, toolName, arguments);
        return result != null ? result : "";
    }

    /**
     * Run tool once. Returns null on exception so caller can retry with fresh client.
     */
    private String executeToolOnce(String serverName, String toolName, Map<String, Object> arguments) {
        if (logger.isDebugEnabled()) {
            logger.debug("MCP executeTool: server={}, tool={}, arguments={}", serverName, toolName, arguments);
        }
        McpSyncClient client = getOrCreateClient(serverName);
        if (client == null) return "";
        try {
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                    toolName,
                    arguments != null ? arguments : Map.of()
            );
            McpSchema.CallToolResult result = client.callTool(request);
            if (result == null || result.content() == null) return "";
            if (Boolean.TRUE.equals(result.isError())) {
                logger.warn("MCP tool {} returned error for server {}", toolName, serverName);
                return "";
            }
            return extractTextFromContent(result.content());
        } catch (Exception e) {
            logger.warn("MCP executeTool failed for server {} tool {}: {}", serverName, toolName, e.getMessage());
            return null; // signal retry
        }
    }

    /** Remove cached client for server so next call creates a fresh connection. */
    private void invalidateClient(String serverName) {
        if (serverName == null || serverName.isBlank()) return;
        McpSyncClient removed = clients.remove(serverName);
        if (removed != null) {
            try {
                removed.closeGracefully();
            } catch (Exception e) {
                logger.debug("Error closing MCP client for {}: {}", serverName, e.getMessage());
            }
        }
    }

    @Override
    public String getDefaultServerName() {
        Map<String, McpProperties.McpServerConfig> all = getAllServerConfigs();
        if (all.isEmpty()) return "";
        return all.keySet().iterator().next();
    }

    /** Resolved config: dynamic overrides static for same name. */
    private Map<String, McpProperties.McpServerConfig> getAllServerConfigs() {
        Map<String, McpProperties.McpServerConfig> out = new LinkedHashMap<>();
        if (properties.getServers() != null) {
            out.putAll(properties.getServers());
        }
        out.putAll(dynamicServers);
        return out;
    }

    private McpProperties.McpServerConfig getServerConfig(String serverName) {
        McpProperties.McpServerConfig dynamic = dynamicServers.get(serverName);
        if (dynamic != null) return dynamic;
        Map<String, McpProperties.McpServerConfig> staticServers = properties.getServers();
        return staticServers != null ? staticServers.get(serverName) : null;
    }

    @Override
    public List<String> getDefaultContextTools(String serverName) {
        McpProperties.McpServerConfig config = getServerConfig(serverName);
        if (config != null && !config.getDefaultContextTools().isEmpty()) {
            return config.getDefaultContextTools();
        }
        return properties.getDefaultContextTools();
    }

    @Override
    public Map<String, List<McpProperties.QueryTransformRule>> getQueryTransforms(String serverName) {
        McpProperties.McpServerConfig config = getServerConfig(serverName);
        if (config != null && !config.getQueryTransforms().isEmpty()) {
            return config.getQueryTransforms();
        }
        return properties.getQueryTransforms();
    }

    private McpSyncClient getOrCreateClient(String serverName) {
        if (serverName == null || serverName.isBlank()) return null;
        McpProperties.McpServerConfig config = getServerConfig(serverName);
        if (config == null || config.getUrl() == null || config.getUrl().isBlank()) return null;
        McpSyncClient existing = clients.get(serverName);
        if (existing != null) return existing;
        McpSyncClient client = createClient(config);
        if (client != null) clients.put(serverName, client);
        return client;
    }

    private McpSyncClient createClient(McpProperties.McpServerConfig config) {
        try {
            var builder = HttpClientStreamableHttpTransport.builder(config.getUrl());
            if (config.getHeaders() != null && !config.getHeaders().isEmpty()) {
                builder.customizeRequest(req -> config.getHeaders().forEach((name, value) -> req.header(name, value)));
            }
            McpClientTransport transport = builder.build();
            McpSyncClient client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()))
                    .build();
            client.initialize();
            logger.info("MCP client connected to {}", config.getUrl());
            return client;
        } catch (Exception e) {
            logger.warn("Failed to create MCP client for {}: {}", config.getUrl(), e.getMessage());
            return null;
        }
    }

    private static String extractTextFromContent(List<McpSchema.Content> content) {
        if (content == null || content.isEmpty()) return "";
        List<String> parts = new ArrayList<>();
        for (McpSchema.Content c : content) {
            if (c instanceof McpSchema.TextContent textContent) {
                String t = textContent.text();
                if (t != null && !t.isBlank()) parts.add(t);
            }
        }
        return String.join("\n", parts);
    }
}
