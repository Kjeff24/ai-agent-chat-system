package com.aiagent.chatsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {
    private String title;
    /** Optional: provider key from registry (e.g. openai, bedrock). If null, default provider is used. */
    private String providerKey;
    /** Optional: model id for the provider (e.g. gpt-4o). If null, provider's default model is used. */
    private String model;
    /** @deprecated Prefer providerKey + model. Kept for backward compatibility. */
    @Deprecated
    private UUID modelConfigId;
}
