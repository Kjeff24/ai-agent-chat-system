package com.aiagent.chatsystem.service.impl;

import com.aiagent.chatsystem.dto.ConversationDTO;
import com.aiagent.chatsystem.dto.CreateConversationRequest;
import com.aiagent.chatsystem.dto.MessageDTO;
import com.aiagent.chatsystem.dto.SendMessageRequest;
import com.aiagent.chatsystem.dto.UpdateConversationRequest;
import com.aiagent.chatsystem.exception.ConversationAccessDeniedException;
import com.aiagent.chatsystem.exception.ConversationNotFoundException;
import com.aiagent.chatsystem.exception.ModelConfigNotFoundException;
import com.aiagent.chatsystem.exception.NoDefaultModelConfigException;
import com.aiagent.chatsystem.model.Conversation;
import com.aiagent.chatsystem.model.Message;
import com.aiagent.chatsystem.model.ModelConfig;
import com.aiagent.chatsystem.repository.ConversationRepository;
import com.aiagent.chatsystem.repository.MessageRepository;
import com.aiagent.chatsystem.repository.ModelConfigRepository;
import com.aiagent.chatsystem.service.AIModelService;
import com.aiagent.chatsystem.service.ConversationService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final AIModelService aiModelService;
    private final SimpMessagingTemplate messagingTemplate;

    public ConversationServiceImpl(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ModelConfigRepository modelConfigRepository,
            AIModelService aiModelService,
            SimpMessagingTemplate messagingTemplate) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.modelConfigRepository = modelConfigRepository;
        this.aiModelService = aiModelService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public List<ConversationDTO> getConversations(UUID userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public ConversationDTO getConversation(UUID id, UUID userId) {
        Conversation c = findConversationOrThrow(id);
        ensureOwnership(c, userId);
        return toDTO(c);
    }

    @Override
    public ConversationDTO createConversation(CreateConversationRequest request, UUID userId) {
        ModelConfig defaultConfig = modelConfigRepository.findByIsDefaultTrueAndIsActiveTrue()
                .orElseThrow(() -> new NoDefaultModelConfigException(
                        "No default model configuration found. Restart the backend to seed one, or add a config via GET /api/models and set it as default."));

        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(request != null ? request.getTitle() : null);
        conversation.setModelConfigId(defaultConfig.getId());
        conversation = conversationRepository.save(conversation);
        return toDTO(conversation);
    }

    @Override
    public List<MessageDTO> getMessages(UUID conversationId, UUID userId) {
        Conversation c = findConversationOrThrow(conversationId);
        ensureOwnership(c, userId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(MessageDTO::fromEntity)
                .toList();
    }

    @Override
    public MessageDTO sendMessage(UUID conversationId, SendMessageRequest request, UUID userId) {
        Conversation conversation = findConversationOrThrow(conversationId);
        ensureOwnership(conversation, userId);

        Message userMessage = new Message();
        userMessage.setConversationId(conversationId);
        userMessage.setRole(Message.MessageRole.user);
        userMessage.setContent(request.getContent());
        userMessage.setMetadata(request.getMetadata());
        userMessage = messageRepository.save(userMessage);

        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, MessageDTO.fromEntity(userMessage));

        List<Message> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (history.size() == 1) {
            String raw = request.getContent() == null ? "" : request.getContent().trim();
            String title = raw.length() <= 50 ? raw : (raw.substring(0, 47) + "...");
            if (!title.isEmpty()) {
                conversation.setTitle(title);
                conversation = conversationRepository.save(conversation);
            }
        }

        UUID modelConfigId = conversation.getModelConfigId();
        List<org.springframework.ai.chat.messages.Message> aiMessages = history.stream()
                .<org.springframework.ai.chat.messages.Message>map(msg -> msg.getRole() == Message.MessageRole.user
                        ? new UserMessage(msg.getContent())
                        : new AssistantMessage(msg.getContent()))
                .toList();

        ModelConfig modelConfig = modelConfigRepository.findById(modelConfigId)
                .orElseThrow(() -> new ModelConfigNotFoundException(modelConfigId));

        String aiResponse = aiModelService.generate(aiMessages, modelConfig).block();

        Message assistantMessage = new Message();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setRole(Message.MessageRole.assistant);
        assistantMessage.setContent(aiResponse);
        assistantMessage = messageRepository.save(assistantMessage);

        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, MessageDTO.fromEntity(assistantMessage));

        return MessageDTO.fromEntity(assistantMessage);
    }

    @Override
    public ConversationDTO updateConversation(UUID id, UpdateConversationRequest request, UUID userId) {
        if (!conversationRepository.existsByIdAndUserId(id, userId)) {
            throw new ConversationAccessDeniedException("Access denied");
        }
        Conversation conversation = findConversationOrThrow(id);
        if (request != null && request.getTitle() != null) {
            String t = request.getTitle().trim();
            conversation.setTitle(t.isEmpty() ? null : t);
        }
        conversation = conversationRepository.save(conversation);
        return toDTO(conversation);
    }

    @Override
    public void deleteConversation(UUID id, UUID userId) {
        if (!conversationRepository.existsByIdAndUserId(id, userId)) {
            throw new ConversationAccessDeniedException("Access denied");
        }
        conversationRepository.deleteById(id);
    }

    private Conversation findConversationOrThrow(UUID id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ConversationNotFoundException(id));
    }

    private void ensureOwnership(Conversation conversation, UUID userId) {
        if (!conversation.getUserId().equals(userId)) {
            throw new ConversationAccessDeniedException("Access denied");
        }
    }

    private ConversationDTO toDTO(Conversation c) {
        return new ConversationDTO(
                c.getId(),
                c.getUserId(),
                c.getTitle(),
                c.getModelConfigId(),
                c.getMetadata(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
