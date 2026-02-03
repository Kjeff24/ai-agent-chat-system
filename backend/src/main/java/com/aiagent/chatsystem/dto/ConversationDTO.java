package com.aiagent.chatsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {
    private UUID id;
    private UUID userId;
    private String title;
    /** Provider key from registry (e.g. openai, bedrock). */
    private String providerKey;
    /** Model id for the provider (e.g. gpt-4o). */
    private String model;
    /** @deprecated Kept for backward compatibility; prefer providerKey + model. */
    @Deprecated
    private UUID modelConfigId;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
