package com.aiagent.chatsystem.service.impl;

import com.aiagent.chatsystem.model.ModelConfig;
import com.aiagent.chatsystem.service.AIModelService;
import com.aiagent.chatsystem.service.ModelRegistry;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
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
    public Flux<String> generateStream(List<Message> messages, String providerKey, String model) {
        ChatModel chatModel = getChatModelByProvider(providerKey);
        Prompt prompt = new Prompt(messages);
        Flux<ChatResponse> responseFlux = chatModel.stream(prompt);
        return responseFlux
                .map(AIModelServiceImpl::extractText)
                .filter(content -> content != null && !content.isEmpty());
    }
    
    @Override
    public Mono<String> generate(List<Message> messages, String providerKey, String model) {
        return generate(messages, providerKey, model, null);
    }

    @Override
    public Mono<String> generate(List<Message> messages, String providerKey, String model, List<ToolCallback> toolCallbacks) {
        ChatModel chatModel = getChatModelByProvider(providerKey);
        Prompt prompt = toolCallbacks != null && !toolCallbacks.isEmpty()
                ? new Prompt(messages, ToolCallingChatOptions.builder().toolCallbacks(toolCallbacks).build())
                : new Prompt(messages);
        ChatResponse response = chatModel.call(prompt);
        String text = extractText(response);
        if (text == null || text.isEmpty()) {
            text = "The model did not return a response. Please try again.";
        }
        return Mono.just(text);
    }

    private static String extractText(ChatResponse response) {
        if (response == null) return null;
        var result = response.getResult();
        if (result == null) return null;
        var output = result.getOutput();
        if (output == null) return null;
        return output.getText();
    }

    @Override
    @Deprecated
    public Flux<String> generateStream(List<Message> messages, ModelConfig config) {
        String providerKey = resolveProviderKeyFromConfig(config);
        return generateStream(messages, providerKey, config.getModel());
    }
    
    @Override
    @Deprecated
    public Mono<String> generate(List<Message> messages, ModelConfig config) {
        return generate(messages, config, null);
    }

    @Override
    @Deprecated
    public Mono<String> generate(List<Message> messages, ModelConfig config, List<ToolCallback> toolCallbacks) {
        String providerKey = resolveProviderKeyFromConfig(config);
        return generate(messages, providerKey, config.getModel(), toolCallbacks);
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
    
    private ChatModel getChatModelByProvider(String providerKey) {
        if (providerKey == null || providerKey.isBlank()) {
            throw new IllegalStateException("Provider key is required. Available providers: " + modelRegistry.getRegisteredProviders());
        }
        String key = providerKey.trim().toLowerCase();
        ChatModel model = modelRegistry.getModel(key);
        if (model == null) {
            throw new IllegalStateException(
                String.format("Chat model for provider '%s' is not available. Available providers: %s",
                    key, modelRegistry.getRegisteredProviders()));
        }
        return model;
    }

    /** Resolve registry key: use parameters.providerKey when provider is custom, else provider enum name. */
    private String resolveProviderKeyFromConfig(ModelConfig config) {
        if (config.getProvider() == ModelConfig.ModelProvider.custom
                && config.getParameters() != null
                && config.getParameters().get("providerKey") != null) {
            String key = String.valueOf(config.getParameters().get("providerKey")).trim();
            if (!key.isEmpty()) return key.toLowerCase();
        }
        return config.getProvider().name().toLowerCase();
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
