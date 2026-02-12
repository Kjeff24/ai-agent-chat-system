package com.aiagent.chatsystem.service;

import com.aiagent.chatsystem.dto.McpServerDetailDTO;
import com.aiagent.chatsystem.dto.McpServerSummaryDTO;
import com.aiagent.chatsystem.dto.RegisterMcpServerRequest;
import com.aiagent.chatsystem.dto.UpdateMcpServerRequest;
import org.springframework.ai.tool.ToolCallback;

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
     * List all configured MCP servers (static from config + dynamically added). Returns summaries with name, url, source, status.
     * Use {@link #listServers(java.util.UUID)} when you have a user id to show OAuth "connected" status.
     */
    List<McpServerSummaryDTO> listServers();

    /**
     * List servers with status for the given user (OAuth servers show "connected" when user has a token).
     */
    List<McpServerSummaryDTO> listServers(java.util.UUID userId);

    /**
     * Get full details of an MCP server by name (for edit). Returns null if not found.
     * Use {@link #getServer(String, java.util.UUID)} when you have a user id for OAuth status.
     */
    McpServerDetailDTO getServer(String name);

    /**
     * Get server details with status for the given user (OAuth "connected" when user has token).
     */
    McpServerDetailDTO getServer(String name, java.util.UUID userId);

    /**
     * Register an MCP server at runtime. Overwrites if the name already exists (static or dynamic). Client is created on next use.
     */
    void addServer(RegisterMcpServerRequest request);

    /**
     * Update a dynamically added MCP server by name. Only provided fields are updated. Fails if server is from static config.
     * Returns true if updated, false if not found or not dynamic.
     */
    boolean updateServer(String name, UpdateMcpServerRequest request);

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
     * Use {@link #executeTool(String, String, Map, java.util.UUID)} when the server uses OAuth.
     */
    String executeTool(String serverName, String toolName, Map<String, Object> arguments);

    /**
     * Execute a tool with user context (for OAuth servers). Pass null for non-OAuth.
     */
    String executeTool(String serverName, String toolName, Map<String, Object> arguments, java.util.UUID userId);

    /**
     * OAuth provider id for a server (e.g. "atlassian"), or null/empty if the server uses static headers.
     */
    String getOAuthProviderForServer(String serverName);

    /**
     * Aggregate MCP tool definitions from all configured servers and return them as Spring AI ToolCallbacks.
     * Used for model-driven tool use: the model sees these tools and decides when to call them.
     * For OAuth servers, pass userId so per-user tokens are used.
     */
    List<ToolCallback> getToolCallbacks();

    /**
     * Same as {@link #getToolCallbacks()} but with user context for OAuth servers (per-user tokens).
     */
    List<ToolCallback> getToolCallbacks(java.util.UUID userId);
}
