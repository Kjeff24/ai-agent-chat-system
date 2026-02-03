package com.aiagent.chatsystem.exception;

/**
 * Thrown when an OAuth provider is not found or is static (e.g. cannot remove/update).
 */
public class McpOAuthProviderNotFoundException extends RuntimeException {

    public McpOAuthProviderNotFoundException(String message) {
        super(message);
    }
}
