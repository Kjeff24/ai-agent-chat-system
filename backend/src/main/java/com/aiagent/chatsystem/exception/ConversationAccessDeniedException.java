package com.aiagent.chatsystem.exception;

public class ConversationAccessDeniedException extends RuntimeException {
    public ConversationAccessDeniedException(String message) {
        super(message);
    }
}
