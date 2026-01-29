package com.aiagent.chatsystem.service;

import com.aiagent.chatsystem.dto.ConversationDTO;
import com.aiagent.chatsystem.dto.MessageDTO;
import com.aiagent.chatsystem.dto.SendMessageRequest;
import com.aiagent.chatsystem.dto.CreateConversationRequest;
import com.aiagent.chatsystem.dto.UpdateConversationRequest;

import java.util.List;
import java.util.UUID;

public interface ConversationService {

    List<ConversationDTO> getConversations(UUID userId);

    ConversationDTO getConversation(UUID id, UUID userId);

    ConversationDTO createConversation(CreateConversationRequest request, UUID userId);

    List<MessageDTO> getMessages(UUID conversationId, UUID userId);

    MessageDTO sendMessage(UUID conversationId, SendMessageRequest request, UUID userId);

    ConversationDTO updateConversation(UUID id, UpdateConversationRequest request, UUID userId);

    void deleteConversation(UUID id, UUID userId);
}
