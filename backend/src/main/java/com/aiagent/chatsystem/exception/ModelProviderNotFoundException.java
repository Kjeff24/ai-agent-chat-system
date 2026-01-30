package com.aiagent.chatsystem.exception;

/**
 * Thrown when a requested model provider is not found or cannot be unregistered (e.g. static-only).
 */
public class ModelProviderNotFoundException extends RuntimeException {

    public ModelProviderNotFoundException(String message) {
        super(message);
    }
}
