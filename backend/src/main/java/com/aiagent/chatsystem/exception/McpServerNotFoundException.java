package com.aiagent.chatsystem.exception;

/**
 * Thrown when a requested MCP server is not found or cannot be removed (e.g. static-only).
 */
public class McpServerNotFoundException extends RuntimeException {

    public McpServerNotFoundException(String message) {
        super(message);
    }
}
