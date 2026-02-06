package com.aiagent.chatsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConversationRequest {
    private String title;
    /** Optional: change conversation's provider (e.g. openai, bedrock). */
    private String providerKey;
    /** Optional: change conversation's model for the provider. */
    private String model;
}
