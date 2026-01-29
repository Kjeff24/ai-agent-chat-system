package com.aiagent.chatsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;
    
    @NotBlank(message = "Message content is required")
    private String content;
    
    private Map<String, Object> metadata;
}
