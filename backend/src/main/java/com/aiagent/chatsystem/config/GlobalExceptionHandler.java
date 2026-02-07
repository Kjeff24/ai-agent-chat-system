package com.aiagent.chatsystem.config;

import com.aiagent.chatsystem.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Pattern OLLAMA_MODEL_NOT_FOUND =
            Pattern.compile("model\\s+(.+?)\\s+not\\s+found", Pattern.CASE_INSENSITIVE);

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleConversationNotFound(ConversationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ConversationAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleConversationAccessDenied(ConversationAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ModelConfigNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleModelConfigNotFound(ModelConfigNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(McpServerNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleMcpServerNotFound(McpServerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(McpOAuthProviderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleMcpOAuthProviderNotFound(McpOAuthProviderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ModelProviderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleModelProviderNotFound(ModelProviderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidModelConfigException.class)
    public ResponseEntity<Map<String, String>> handleInvalidModelConfig(InvalidModelConfigException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NoDefaultModelConfigException.class)
    public ResponseEntity<Map<String, String>> handleNoDefaultModelConfig(NoDefaultModelConfigException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NoDefaultProviderException.class)
    public ResponseEntity<Map<String, String>> handleNoDefaultProvider(NoDefaultProviderException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, String>> handleUnsupportedOperation(UnsupportedOperationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleOllamaErrors(RuntimeException ex) {
        String msg = ex.getMessage();
        if (msg == null) {
            throw ex;
        }

        if (msg.contains("try pulling")) {
            Matcher m = OLLAMA_MODEL_NOT_FOUND.matcher(msg);
            String model = m.find() ? m.group(1).replaceAll("^[\"\\\\]+|[\"\\\\]+$", "").trim() : "unknown";
            String body = "Ollama model '" + model + "' not found. Run: ollama pull " + model;
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", body));
        }

        if (msg.contains("llama runner process has terminated") && msg.contains("signal: killed")) {
            String body = "Ollama's model runner was killed (often due to low memory). Try restarting Ollama, using a smaller model, or freeing system RAM.";
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", body));
        }

        throw ex;
    }
}
