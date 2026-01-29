package com.aiagent.chatsystem.dto;

import com.aiagent.chatsystem.model.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private UUID id;
    private UUID conversationId;
    private Message.MessageRole role;
    private String content;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    
    public static MessageDTO fromEntity(Message message) {
        return new MessageDTO(
            message.getId(),
            message.getConversationId(),
            message.getRole(),
            message.getContent(),
            message.getMetadata(),
            message.getCreatedAt()
        );
    }
}
