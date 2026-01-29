package com.aiagent.chatsystem.service.impl;

import com.aiagent.chatsystem.model.ModelConfig;
import com.aiagent.chatsystem.service.AIModelService;
import com.aiagent.chatsystem.service.ModelRegistry;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class AIModelServiceImpl implements AIModelService {
    
    private final ModelRegistry modelRegistry;
    
    @Autowired
    public AIModelServiceImpl(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }
    
    @Override
    public Flux<String> generateStream(List<Message> messages, ModelConfig config) {
        ChatModel chatModel = getChatModel(config);
        Prompt prompt = getPromptWithOptions(messages, config);
        
        Flux<ChatResponse> responseFlux = chatModel.stream(prompt);
        
        return responseFlux
                .map(response -> response.getResult().getOutput().getContent())
                .filter(content -> content != null && !content.isEmpty());
    }
    
    @Override
    public Mono<String> generate(List<Message> messages, ModelConfig config) {
        ChatModel chatModel = getChatModel(config);
        Prompt prompt = getPromptWithOptions(messages, config);
        
        ChatResponse response = chatModel.call(prompt);
        return Mono.just(response.getResult().getOutput().getContent());
    }
    
    @Override
    public boolean validateConfig(ModelConfig config) {
        if (config == null || config.getModel() == null || config.getProvider() == null) {
            return false;
        }
        
        Map<String, Object> params = config.getParameters();
        if (params != null) {
            // Validate temperature
            if (params.containsKey("temperature")) {
                Double temp = getDoubleValue(params.get("temperature"));
                if (temp == null || temp < 0 || temp > 2) {
                    return false;
                }
            }
            
            // Validate maxTokens
            if (params.containsKey("maxTokens")) {
                Integer maxTokens = getIntegerValue(params.get("maxTokens"));
                if (maxTokens == null || maxTokens < 1 || maxTokens > 100000) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private ChatModel getChatModel(ModelConfig config) {
        String provider = config.getProvider().name().toLowerCase();
        ChatModel model = modelRegistry.getModel(provider);
        
        if (model == null) {
            throw new IllegalStateException(
                String.format("Chat model for provider '%s' is not available. " +
                    "Please configure the provider (e.g., set API key for %s or start Ollama service). " +
                    "Available providers: %s", 
                    provider, provider, modelRegistry.getRegisteredProviders()));
        }
        
        return model;
    }
    
    private Prompt getPromptWithOptions(List<Message> messages, ModelConfig config) {
        Map<String, Object> params = config.getParameters();
        Prompt prompt = new Prompt(messages);
        
        // Note: In Spring AI 1.0.0-M4, options are typically set via the ChatModel's default options
        // or through the application.yml configuration. For runtime configuration changes,
        // you may need to create new ChatModel instances with different options.
        // For now, we'll use the default configured models.
        
        return prompt;
    }
    
    private Double getDoubleValue(Object value) {
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private Integer getIntegerValue(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
