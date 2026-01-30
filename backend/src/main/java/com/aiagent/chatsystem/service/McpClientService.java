package com.aiagent.chatsystem.service;

import com.aiagent.chatsystem.config.McpProperties;
import com.aiagent.chatsystem.dto.McpServerSummaryDTO;
import com.aiagent.chatsystem.dto.RegisterMcpServerRequest;

import java.util.List;
import java.util.Map;

/**
 * Service for connecting to external MCP servers (Streamable HTTP) and
 * listing/executing tools (and optionally reading resources) for chat context.
 */
public interface McpClientService {

    /**
     * Whether MCP is enabled and at least one server is configured.
     */
    boolean isEnabled();

    /**
     * List all configured MCP servers (static from config + dynamically added). Returns summaries with name, url, source.
     */
    List<McpServerSummaryDTO> listServers();

    /**
     * Register an MCP server at runtime. Overwrites if the name already exists (static or dynamic). Client is created on next use.
     */
    void addServer(RegisterMcpServerRequest request);

    /**
     * Remove a dynamically added MCP server by name. Fails if the server is only from static config (use config/file to remove those).
     * Returns true if removed, false if not found or not dynamic.
     */
    boolean removeServer(String name);

    /**
     * List available tool names for a server. Returns empty list if server not configured or on error.
     */
    List<String> listToolNames(String serverName);

    /**
     * Execute a tool on an MCP server and return the result as plain text for context injection.
     * Returns empty string on error or if server/tool not available.
     */
    String executeTool(String serverName, String toolName, Map<String, Object> arguments);

    /**
     * Get the first configured server name, or empty if none.
     */
    String getDefaultServerName();

    /**
     * Get the default context tool names for a server. Uses that server's default-context-tool if set, otherwise top-level fallback.
     * When non-empty, each tool is called with the last user message and results are combined as context.
     */
    List<String> getDefaultContextTools(String serverName);

    /**
     * Get per-tool query transforms for a server. Uses that server's query-transforms if set, otherwise top-level fallback.
     * Key = tool name; value = list of { pattern, template }.
     */
    Map<String, List<McpProperties.QueryTransformRule>> getQueryTransforms(String serverName);
}
