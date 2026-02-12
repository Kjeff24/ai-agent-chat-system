package com.aiagent.chatsystem.service.impl;

import com.aiagent.chatsystem.config.AppMcpOAuthProperties;
import com.aiagent.chatsystem.config.McpProperties;
import com.aiagent.chatsystem.dto.McpServerDetailDTO;
import com.aiagent.chatsystem.model.McpServer;
import com.aiagent.chatsystem.repository.McpServerRepository;
import com.aiagent.chatsystem.service.McpOAuthService;
import com.aiagent.chatsystem.dto.McpServerSummaryDTO;
import com.aiagent.chatsystem.dto.RegisterMcpServerRequest;
import com.aiagent.chatsystem.dto.UpdateMcpServerRequest;
import com.aiagent.chatsystem.service.McpClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP client service using Streamable HTTP transport.
 * Lazily creates and caches one McpSyncClient per configured server.
 * Supports static config (application.yml) and dynamic registration via API.
 * Dynamic servers are persisted in the database and restored on startup.
 */
@Service
public class McpClientServiceImpl implements McpClientService {

    private static final Logger logger = LoggerFactory.getLogger(McpClientServiceImpl.class);

    /** Full initialize params for MCP handshake (required by some servers e.g. Atlassian). */
    private static final McpSchema.ClientCapabilities MCP_CLIENT_CAPABILITIES = McpSchema.ClientCapabilities.builder().build();
    private static final McpSchema.Implementation MCP_CLIENT_INFO = new McpSchema.Implementation("ai-agent-chat-system", "1.0");

    /**
     * Split full MCP URL into (baseUri, endpoint) so that transport's resolveUri(baseUri, endpoint) equals the config URL.
     * The SDK uses baseUri + endpoint (default "/mcp"); if we pass the full URL as base, resolve(base, "/mcp") becomes wrong path.
     */
    private static String[] transportBaseAndEndpoint(String fullUrl) {
        if (fullUrl == null || fullUrl.isBlank()) return new String[] { fullUrl, "/mcp" };
        URI u = URI.create(fullUrl.trim());
        String base = u.getScheme() + "://" + u.getAuthority();
        String path = u.getPath();
        if (path == null || path.isEmpty()) path = "/mcp";
        if (!path.startsWith("/")) path = "/" + path;
        return new String[] { base, path };
    }

    private final McpProperties properties;
    private final AppMcpOAuthProperties oauthProperties;
    private final McpOAuthService mcpOAuthService;
    private final McpServerRepository mcpServerRepository;
    private final Map<String, McpSyncClient> clients = new ConcurrentHashMap<>();
    private final Map<String, McpProperties.McpServerConfig> dynamicServers = new ConcurrentHashMap<>();
    /** Per-server connection status: "connected" | "failed". Absent = not yet probed. */
    private final Map<String, String> serverStatus = new ConcurrentHashMap<>();
    /** OAuth client creation failures: key = "userId_serverName", value = expiry time (ms). Used so list/get can show "failed". */
    private final Map<String, Long> oauthFailureExpiry = new ConcurrentHashMap<>();
    private static final long OAUTH_FAILURE_CACHE_MS = 5 * 60 * 1000; // 5 minutes

    public McpClientServiceImpl(McpProperties properties, AppMcpOAuthProperties oauthProperties,
                                McpOAuthService mcpOAuthService, McpServerRepository mcpServerRepository) {
        this.properties = properties;
        this.oauthProperties = oauthProperties;
        this.mcpOAuthService = mcpOAuthService;
        this.mcpServerRepository = mcpServerRepository;
    }

    @PostConstruct
    public void loadDynamicServersFromDb() {
        for (McpServer entity : mcpServerRepository.findAllByOrderByCreatedAtAsc()) {
            McpProperties.McpServerConfig config = entityToConfig(entity);
            dynamicServers.put(entity.getName(), config);
        }
        if (!dynamicServers.isEmpty()) {
            logger.info("Loaded {} MCP server(s) from database", dynamicServers.size());
        }
    }

    private static McpProperties.McpServerConfig entityToConfig(McpServer entity) {
        McpProperties.McpServerConfig config = new McpProperties.McpServerConfig();
        config.setUrl(entity.getUrl());
        config.setRequestTimeoutSeconds(entity.getRequestTimeoutSeconds() > 0 ? entity.getRequestTimeoutSeconds() : 30);
        config.setHeaders(entity.getHeaders() != null ? new LinkedHashMap<>(entity.getHeaders()) : new LinkedHashMap<>());
        config.setOauthProvider(entity.getOauthProvider() != null ? entity.getOauthProvider() : "");
        return config;
    }

    @Override
    public boolean isEnabled() {
        return !getAllServerConfigs().isEmpty();
    }

    @Override
    public List<McpServerSummaryDTO> listServers() {
        return listServers(null);
    }

    @Override
    public List<McpServerSummaryDTO> listServers(UUID userId) {
        List<McpServerSummaryDTO> result = new ArrayList<>();
        Map<String, McpProperties.McpServerConfig> staticServers = properties.getServers();
        if (staticServers != null) {
            for (Map.Entry<String, McpProperties.McpServerConfig> e : staticServers.entrySet()) {
                if (e.getValue() != null && e.getValue().getUrl() != null) {
                    McpServerSummaryDTO dto = new McpServerSummaryDTO(e.getKey(), e.getValue().getUrl(), "static");
                    enrichStatusAndOAuth(dto, e.getKey(), e.getValue(), userId);
                    result.add(dto);
                }
            }
        }
        for (Map.Entry<String, McpProperties.McpServerConfig> e : dynamicServers.entrySet()) {
            if (e.getValue() != null && e.getValue().getUrl() != null) {
                McpServerSummaryDTO dto = new McpServerSummaryDTO(e.getKey(), e.getValue().getUrl(), "dynamic");
                enrichStatusAndOAuth(dto, e.getKey(), e.getValue(), userId);
                result.add(dto);
            }
        }
        return result;
    }

    private void enrichStatusAndOAuth(McpServerSummaryDTO dto, String serverName, McpProperties.McpServerConfig config, UUID userId) {
        String op = config.getOauthProvider();
        if (op != null && !op.isBlank()) {
            dto.setOauthProvider(op);
            if (userId == null || !mcpOAuthService.hasToken(userId, serverName)) {
                dto.setStatus("not_authorized");
            } else {
                String token = mcpOAuthService.getValidAccessToken(userId, serverName);
                dto.setStatus(token != null && !token.isBlank() ? "connected" : "failed");
            }
        } else {
            dto.setStatus(serverStatus.getOrDefault(serverName, "unknown"));
        }
    }

    private static String oauthFailureKey(UUID userId, String serverName) {
        return userId + "_" + serverName;
    }

    private void recordOAuthFailure(UUID userId, String serverName) {
        if (userId == null) return;
        oauthFailureExpiry.put(oauthFailureKey(userId, serverName), System.currentTimeMillis() + OAUTH_FAILURE_CACHE_MS);
    }

    @Override
    public McpServerDetailDTO getServer(String name) {
        return getServer(name, null);
    }

    @Override
    public McpServerDetailDTO getServer(String name, UUID userId) {
        if (name == null || name.isBlank()) return null;
        McpProperties.McpServerConfig config = getServerConfig(name);
        if (config == null || config.getUrl() == null) return null;
        McpServerDetailDTO dto = new McpServerDetailDTO();
        dto.setName(name);
        dto.setUrl(config.getUrl());
        dto.setSource(dynamicServers.containsKey(name) ? "dynamic" : "static");
        String op = config.getOauthProvider();
        if (op != null && !op.isBlank()) {
            dto.setOauthProvider(op);
            if (userId == null || !mcpOAuthService.hasToken(userId, name)) {
                dto.setStatus("not_authorized");
            } else {
                String token = mcpOAuthService.getValidAccessToken(userId, name);
                dto.setStatus(token != null && !token.isBlank() ? "connected" : "failed");
            }
        } else {
            dto.setStatus(serverStatus.getOrDefault(name, "unknown"));
        }
        dto.setRequestTimeoutSeconds(config.getRequestTimeoutSeconds());
        if (config.getHeaders() != null && !config.getHeaders().isEmpty()) {
            dto.setHeaders(new LinkedHashMap<>(config.getHeaders()));
        }
        return dto;
    }

    @Override
    @Transactional
    public void addServer(RegisterMcpServerRequest request) {
        String name = request.getName().trim();
        McpServer entity = new McpServer();
        entity.setName(name);
        entity.setUrl(request.getUrl().trim());
        entity.setRequestTimeoutSeconds(request.getRequestTimeoutSeconds() > 0 ? request.getRequestTimeoutSeconds() : 30);
        entity.setHeaders(request.getHeaders() != null && !request.getHeaders().isEmpty()
                ? new LinkedHashMap<>(request.getHeaders()) : new LinkedHashMap<>());
        entity.setOauthProvider(request.getOauthProvider() != null ? request.getOauthProvider().trim() : null);
        mcpServerRepository.save(entity);

        McpProperties.McpServerConfig config = entityToConfig(entity);
        dynamicServers.put(name, config);
        clients.remove(name);
        serverStatus.remove(name);
        logger.info("MCP server registered dynamically: {} -> {}", name, config.getUrl());
    }

    @Override
    @Transactional
    public boolean updateServer(String name, UpdateMcpServerRequest request) {
        if (name == null || name.isBlank()) return false;
        McpServer entity = mcpServerRepository.findByName(name).orElse(null);
        if (entity == null) return false;
        if (request.getUrl() != null && !request.getUrl().isBlank()) {
            entity.setUrl(request.getUrl().trim());
        }
        if (request.getRequestTimeoutSeconds() != null && request.getRequestTimeoutSeconds() > 0) {
            entity.setRequestTimeoutSeconds(request.getRequestTimeoutSeconds());
        }
        if (request.getHeaders() != null) {
            entity.setHeaders(request.getHeaders().isEmpty() ? new LinkedHashMap<>() : new LinkedHashMap<>(request.getHeaders()));
        }
        if (request.getOauthProvider() != null) {
            entity.setOauthProvider(request.getOauthProvider().trim().isEmpty() ? null : request.getOauthProvider().trim());
        }
        mcpServerRepository.save(entity);

        McpProperties.McpServerConfig config = entityToConfig(entity);
        dynamicServers.put(name, config);
        clients.remove(name);
        serverStatus.remove(name);
        logger.info("MCP server updated: {}", name);
        return true;
    }

    @Override
    @Transactional
    public boolean removeServer(String name) {
        if (name == null || name.isBlank()) return false;
        if (!mcpServerRepository.existsByName(name)) return false;
        mcpServerRepository.deleteByName(name);
        dynamicServers.remove(name);
        serverStatus.remove(name);
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
        McpSyncClient client = getOrCreateClient(serverName, null);
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
        return executeTool(serverName, toolName, arguments, null);
    }

    @Override
    public String executeTool(String serverName, String toolName, Map<String, Object> arguments, UUID userId) {
        String result = executeToolOnce(serverName, toolName, arguments, userId);
        if (result != null) return result;
        // Retry once with a fresh client (helps recover from 405/SSE leaving session in bad state)
        logger.info("MCP executeTool retrying with fresh client for server {} tool {}", serverName, toolName);
        invalidateClient(serverName, userId);
        result = executeToolOnce(serverName, toolName, arguments, userId);
        return result != null ? result : "";
    }

    /**
     * Run tool once. Returns null on exception so caller can retry with fresh client.
     */
    private String executeToolOnce(String serverName, String toolName, Map<String, Object> arguments, UUID userId) {
        if (logger.isDebugEnabled()) {
            logger.debug("MCP executeTool: server={}, tool={}, arguments={}", serverName, toolName, arguments);
        }
        McpSyncClient client = getOrCreateClient(serverName, userId);
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

    /** Remove cached client for server so next call creates a fresh connection. (OAuth servers are not cached.) */
    private void invalidateClient(String serverName, UUID userId) {
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
    public String getOAuthProviderForServer(String serverName) {
        McpProperties.McpServerConfig config = getServerConfig(serverName);
        if (config == null) return null;
        String op = config.getOauthProvider();
        return (op == null || op.isBlank()) ? null : op;
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
    public List<ToolCallback> getToolCallbacks() {
        return getToolCallbacks(null);
    }

    @Override
    public List<ToolCallback> getToolCallbacks(UUID userId) {
        List<ToolCallback> callbacks = new ArrayList<>();
        ObjectMapper om = new ObjectMapper();
        for (String serverName : getAllServerConfigs().keySet()) {
            McpSyncClient client = getOrCreateClient(serverName, userId);
            if (client == null) {
                logger.debug("Skipping MCP server {}: client could not be created (check OAuth token or server connectivity)", serverName);
                continue;
            }
            try {
                McpSchema.ListToolsResult result = client.listTools();
                if (result == null || result.tools() == null) continue;
                for (McpSchema.Tool tool : result.tools()) {
                    String name = tool.name();
                    if (name == null || name.isBlank()) continue;
                    String description = toolDescription(tool);
                    String inputSchema = toolInputSchema(tool, om);
                    callbacks.add(new McpToolCallbackAdapter(serverName, name, description, inputSchema, this, userId));
                }
            } catch (Exception e) {
                logger.warn("MCP listTools failed for server {}: {}", serverName, e.getMessage());
            }
        }
        return callbacks;
    }

    private static String toolDescription(McpSchema.Tool tool) {
        try {
            if (tool.description() != null && !tool.description().isBlank()) {
                return tool.description();
            }
        } catch (Exception ignored) { }
        return "MCP tool: " + tool.name();
    }

    private static String toolInputSchema(McpSchema.Tool tool, ObjectMapper om) {
        try {
            Object schema = tool.inputSchema();
            if (schema == null) return "";
            if (schema instanceof String s) return s;
            return om.writeValueAsString(schema);
        } catch (Exception e) {
            return "";
        }
    }

    private McpSyncClient getOrCreateClient(String serverName, UUID userId) {
        if (serverName == null || serverName.isBlank()) return null;
        McpProperties.McpServerConfig config = getServerConfig(serverName);
        if (config == null || config.getUrl() == null || config.getUrl().isBlank()) return null;
        String op = config.getOauthProvider();
        if (op != null && !op.isBlank() && userId != null) {
            String token = mcpOAuthService.getValidAccessToken(userId, serverName);
            if (token == null || token.isBlank()) return null;
            McpSyncClient client = createClientWithToken(config, token, serverName, userId);
            if (client == null) recordOAuthFailure(userId, serverName);
            return client; // no cache for OAuth (token may expire)
        }
        String key = (userId == null) ? serverName : serverName; // non-OAuth: cache by serverName only
        McpSyncClient existing = clients.get(key);
        if (existing != null) return existing;
        McpSyncClient client = createClient(config);
        serverStatus.put(serverName, client != null ? "connected" : "failed");
        if (client != null) clients.put(key, client);
        return client;
    }

    private McpSyncClient createClient(McpProperties.McpServerConfig config) {
        try {
            String configUrl = config.getUrl();
            String[] baseAndEndpoint = transportBaseAndEndpoint(configUrl);
            var builder = HttpClientStreamableHttpTransport.builder(baseAndEndpoint[0]);
            builder.endpoint(baseAndEndpoint[1]);
            if (config.getHeaders() != null && !config.getHeaders().isEmpty()) {
                builder.customizeRequest(req -> config.getHeaders().forEach((name, value) -> req.header(name, value)));
            }
            McpClientTransport transport = builder.build();
            McpSyncClient client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()))
                    .capabilities(MCP_CLIENT_CAPABILITIES)
                    .clientInfo(MCP_CLIENT_INFO)
                    .build();
            client.initialize();
            logger.info("MCP client connected to {}", config.getUrl());
            return client;
        } catch (Exception e) {
            logger.warn("Failed to create MCP client for {}: {}", config.getUrl(), e.getMessage());
            return null;
        }
    }

    private McpSyncClient createClientWithToken(McpProperties.McpServerConfig config, String accessToken,
                                                 String serverName, UUID userId) {
        try {
            String configUrl = config.getUrl();
            String[] baseAndEndpoint = transportBaseAndEndpoint(configUrl);
            String transportBase = baseAndEndpoint[0];
            String transportEndpoint = baseAndEndpoint[1];
            var builder = HttpClientStreamableHttpTransport.builder(transportBase);
            builder.endpoint(transportEndpoint);
            String callbackOrigin = getCallbackOrigin();
            builder.customizeRequest(req -> {
                req.header("Authorization", "Bearer " + accessToken);
                if (callbackOrigin != null) {
                    req.header("Origin", callbackOrigin);
                }
            });
            McpClientTransport transport = builder.build();
            McpSyncClient client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()))
                    .capabilities(MCP_CLIENT_CAPABILITIES)
                    .clientInfo(MCP_CLIENT_INFO)
                    .build();
            client.initialize();
            logger.debug("MCP OAuth client connected to {}", config.getUrl());
            return client;
        } catch (Exception e) {
            logger.warn("Failed to create MCP OAuth client for {}: {}", config.getUrl(), e.getMessage());
            String causeMsg = e.getCause() != null ? e.getCause().getMessage() : "";
            if (causeMsg != null && (causeMsg.contains("404") || causeMsg.contains("Server Not Found"))) {
                String origin = getCallbackOrigin();
                if (origin != null) {
                    logger.info("OAuth MCP returned 404. If the server allowlists by domain, add this origin to its allowed list: [{}]", origin);
                }
            }
            if (logger.isDebugEnabled()) {
                Throwable cause = e.getCause();
                if (cause != null) {
                    logger.debug("OAuth MCP client failure cause: {}", cause.getMessage());
                }
                logger.debug("Full exception", e);
            }
            return null;
        }
    }

    /** Origin (scheme + host + port) from OAuth callback URI; null if not configured. Used for Origin header on OAuth MCP requests. */
    private String getCallbackOrigin() {
        if (oauthProperties == null) return null;
        String callbackUri = oauthProperties.getCallbackUri();
        if (callbackUri == null || callbackUri.isBlank()) return null;
        int afterScheme = callbackUri.indexOf("://");
        if (afterScheme < 0) return null;
        int pathStart = callbackUri.indexOf("/", afterScheme + 3);
        return pathStart > 0 ? callbackUri.substring(0, pathStart) : callbackUri;
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
