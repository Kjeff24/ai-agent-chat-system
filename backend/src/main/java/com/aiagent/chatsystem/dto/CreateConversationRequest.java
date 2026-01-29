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
    private UUID modelConfigId;
}
