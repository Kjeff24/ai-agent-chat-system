package com.aiagent.chatsystem.service;

import com.aiagent.chatsystem.model.ModelConfig;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AIModelService {
    /**
     * Generate a streaming response from AI model using provider key (and optional model id).
     */
    Flux<String> generateStream(List<Message> messages, String providerKey, String model);

    /**
     * Generate a complete response from AI model using provider key (and optional model id).
     */
    Mono<String> generate(List<Message> messages, String providerKey, String model);

    /**
     * Generate a complete response from AI model with tools, using provider key (and optional model id).
     */
    Mono<String> generate(List<Message> messages, String providerKey, String model, List<ToolCallback> toolCallbacks);

    /**
     * @deprecated Use {@link #generate(List, String, String)} with providerKey and model from conversation.
     */
    @Deprecated
    Flux<String> generateStream(List<Message> messages, ModelConfig config);

    /**
     * @deprecated Use {@link #generate(List, String, String)}.
     */
    @Deprecated
    Mono<String> generate(List<Message> messages, ModelConfig config);

    /**
     * @deprecated Use {@link #generate(List, String, String, List)}.
     */
    @Deprecated
    Mono<String> generate(List<Message> messages, ModelConfig config, List<ToolCallback> toolCallbacks);

    /**
     * Validate model configuration
     */
    boolean validateConfig(ModelConfig config);
}
