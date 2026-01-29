package com.aiagent.chatsystem.exception;

import java.util.UUID;

public class ModelConfigNotFoundException extends RuntimeException {
    public ModelConfigNotFoundException(UUID id) {
        super("Model config not found: " + id);
    }
}
