package com.aiagent.chatsystem.service.impl;

import com.aiagent.chatsystem.service.McpClientService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Adapts an MCP server tool to Spring AI's ToolCallback so the model can decide when to call it.
 * Tool names are prefixed with "mcp_{serverName}__" to avoid collisions across servers.
 */
public class McpToolCallbackAdapter implements ToolCallback {

    private static final Logger logger = LoggerFactory.getLogger(McpToolCallbackAdapter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String serverName;
    private final String toolName;
    private final String uniqueName;
    private final String description;
    private final String inputSchema;
    private final McpClientService mcpClientService;
    private final UUID userId;

    public McpToolCallbackAdapter(String serverName, String toolName, String description, String inputSchema,
                                   McpClientService mcpClientService) {
        this(serverName, toolName, description, inputSchema, mcpClientService, null);
    }

    public McpToolCallbackAdapter(String serverName, String toolName, String description, String inputSchema,
                                   McpClientService mcpClientService, UUID userId) {
        this.serverName = serverName;
        this.toolName = toolName;
        this.uniqueName = "mcp_" + sanitize(serverName) + "__" + sanitize(toolName);
        this.description = description != null && !description.isBlank() ? description : "MCP tool: " + toolName;
        this.inputSchema = inputSchema != null && !inputSchema.isBlank() ? inputSchema : defaultInputSchema();
        this.mcpClientService = mcpClientService;
        this.userId = userId;
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        return s.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private static String defaultInputSchema() {
        return """
            {"type":"object","properties":{"query":{"type":"string","description":"User query or search term"}},"required":[]}
            """;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name(uniqueName)
                .description(description)
                .inputSchema(inputSchema)
                .build();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        Map<String, Object> args = parseInput(toolInput);
        try {
            String result = mcpClientService.executeTool(serverName, toolName, args, userId);
            return result != null ? result : "";
        } catch (Exception e) {
            logger.warn("MCP tool call failed: server={}, tool={}, error={}", serverName, toolName, e.getMessage());
            return "Tool call failed: " + e.getMessage();
        }
    }

    private Map<String, Object> parseInput(String toolInput) {
        if (toolInput == null || toolInput.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(toolInput, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            logger.debug("Could not parse tool input as JSON, using empty map: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
