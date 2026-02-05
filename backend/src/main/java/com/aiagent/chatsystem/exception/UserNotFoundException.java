package com.aiagent.chatsystem.exception;

/**
 * Thrown when the authenticated user (from JWT) is not present in the database,
 * e.g. after a DB reset. Client should log in again.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
