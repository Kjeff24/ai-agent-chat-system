package com.aiagent.chatsystem.service.impl;

import com.aiagent.chatsystem.dto.ConversationDTO;
import com.aiagent.chatsystem.dto.CreateConversationRequest;
import com.aiagent.chatsystem.dto.MessageDTO;
import com.aiagent.chatsystem.dto.SendMessageRequest;
import com.aiagent.chatsystem.dto.UpdateConversationRequest;
import com.aiagent.chatsystem.exception.ConversationAccessDeniedException;
import com.aiagent.chatsystem.exception.ConversationNotFoundException;
import com.aiagent.chatsystem.exception.ModelConfigNotFoundException;
import com.aiagent.chatsystem.exception.NoDefaultProviderException;
import com.aiagent.chatsystem.model.Conversation;
import com.aiagent.chatsystem.model.Message;
import com.aiagent.chatsystem.model.ModelConfig;
import com.aiagent.chatsystem.repository.ConversationRepository;
import com.aiagent.chatsystem.repository.MessageRepository;
import com.aiagent.chatsystem.repository.ModelConfigRepository;
import com.aiagent.chatsystem.service.AIModelService;
import com.aiagent.chatsystem.service.ConversationService;
import com.aiagent.chatsystem.service.McpClientService;
import com.aiagent.chatsystem.service.ModelRegistry;
import com.aiagent.chatsystem.service.SystemPromptService;
import org.springframework.ai.tool.ToolCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ConversationServiceImpl implements ConversationService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationServiceImpl.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final ModelRegistry modelRegistry;
    private final AIModelService aiModelService;
    private final McpClientService mcpClientService;
    private final SystemPromptService systemPromptService;
    private final SimpMessagingTemplate messagingTemplate;

    public ConversationServiceImpl(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ModelConfigRepository modelConfigRepository,
            ModelRegistry modelRegistry,
            AIModelService aiModelService,
            McpClientService mcpClientService,
            SystemPromptService systemPromptService,
            SimpMessagingTemplate messagingTemplate) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.modelConfigRepository = modelConfigRepository;
        this.modelRegistry = modelRegistry;
        this.aiModelService = aiModelService;
        this.mcpClientService = mcpClientService;
        this.systemPromptService = systemPromptService;
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
    @Transactional
    public ConversationDTO createConversation(CreateConversationRequest request, UUID userId) {
        String providerKey;
        String model;
        if (request != null && request.getProviderKey() != null && !request.getProviderKey().isBlank()) {
            providerKey = request.getProviderKey().trim().toLowerCase();
            model = (request.getModel() != null && !request.getModel().isBlank())
                    ? request.getModel().trim()
                    : modelRegistry.getDefaultModel(providerKey);
        } else {
            providerKey = modelRegistry.getDefaultProviderKey();
            if (providerKey == null) {
                throw new NoDefaultProviderException(
                        "No default model provider found. Add a provider in Settings (e.g. OpenAI, Ollama, Bedrock).");
            }
            model = modelRegistry.getDefaultModel(providerKey);
        }

        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(request != null ? request.getTitle() : null);
        conversation.setProviderKey(providerKey);
        conversation.setModel(model);
        conversation.setModelConfigId(null);
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
    @Transactional
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

        String providerKey;
        String model;
        if (conversation.getProviderKey() != null && !conversation.getProviderKey().isBlank()) {
            providerKey = conversation.getProviderKey().trim().toLowerCase();
            model = conversation.getModel() != null ? conversation.getModel().trim() : null;
            if (model != null && model.isBlank()) model = null;
        } else {
            // Backward compatibility: resolve from legacy modelConfigId
            UUID modelConfigId = conversation.getModelConfigId();
            if (modelConfigId == null) {
                throw new IllegalStateException(
                        "Conversation has no provider/model and no legacy modelConfigId. Update the conversation with a provider and model.");
            }
            ModelConfig modelConfig = modelConfigRepository.findById(modelConfigId)
                    .orElseThrow(() -> new ModelConfigNotFoundException(modelConfigId));
            providerKey = modelConfigToProviderKey(modelConfig);
            model = modelConfig.getModel();
        }

        List<org.springframework.ai.chat.messages.Message> aiMessages = history.stream()
                .<org.springframework.ai.chat.messages.Message>map(msg -> msg.getRole() == Message.MessageRole.user
                        ? new UserMessage(msg.getContent())
                        : new AssistantMessage(msg.getContent()))
                .toList();

        String systemPrompt = systemPromptService.getEffectiveSystemPrompt();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            List<org.springframework.ai.chat.messages.Message> withSystem = new ArrayList<>(aiMessages.size() + 1);
            withSystem.add(new SystemMessage(systemPrompt));
            withSystem.addAll(aiMessages);
            aiMessages = withSystem;
        }

        String aiResponse;
        if (mcpClientService.isEnabled()) {
            List<ToolCallback> toolCallbacks = mcpClientService.getToolCallbacks(userId);
            if (!toolCallbacks.isEmpty()) {
                logger.debug("MCP model-driven tools: {} tool(s) available to model", toolCallbacks.size());
                aiResponse = aiModelService.generate(aiMessages, providerKey, model, toolCallbacks).block();
            } else {
                aiResponse = aiModelService.generate(aiMessages, providerKey, model).block();
            }
        } else {
            aiResponse = aiModelService.generate(aiMessages, providerKey, model).block();
        }

        Message assistantMessage = new Message();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setRole(Message.MessageRole.assistant);
        assistantMessage.setContent(aiResponse);
        assistantMessage = messageRepository.save(assistantMessage);

        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, MessageDTO.fromEntity(assistantMessage));

        return MessageDTO.fromEntity(assistantMessage);
    }

    @Override
    @Transactional
    public ConversationDTO updateConversation(UUID id, UpdateConversationRequest request, UUID userId) {
        if (!conversationRepository.existsByIdAndUserId(id, userId)) {
            throw new ConversationAccessDeniedException("Access denied");
        }
        Conversation conversation = findConversationOrThrow(id);
        if (request != null) {
            if (request.getTitle() != null) {
                String t = request.getTitle().trim();
                conversation.setTitle(t.isEmpty() ? null : t);
            }
            if (request.getProviderKey() != null) {
                String pk = request.getProviderKey().trim();
                if (!pk.isEmpty()) conversation.setProviderKey(pk.toLowerCase());
            }
            if (request.getModel() != null) {
                String m = request.getModel().trim();
                conversation.setModel(m.isEmpty() ? null : m);
            }
        }
        conversation = conversationRepository.save(conversation);
        return toDTO(conversation);
    }

    @Override
    @Transactional
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
                c.getProviderKey(),
                c.getModel(),
                c.getModelConfigId(),
                c.getMetadata(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    /** Resolve registry provider key from legacy ModelConfig. */
    private String modelConfigToProviderKey(ModelConfig config) {
        if (config.getProvider() == ModelConfig.ModelProvider.custom
                && config.getParameters() != null
                && config.getParameters().get("providerKey") != null) {
            String key = String.valueOf(config.getParameters().get("providerKey")).trim();
            if (!key.isEmpty()) return key.toLowerCase();
        }
        return config.getProvider().name().toLowerCase();
    }
}
