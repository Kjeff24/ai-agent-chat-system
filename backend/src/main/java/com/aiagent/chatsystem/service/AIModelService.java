package com.aiagent.chatsystem.service;

import com.aiagent.chatsystem.model.ModelConfig;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AIModelService {
    /**
     * Generate a streaming response from AI model
     */
    Flux<String> generateStream(List<Message> messages, ModelConfig config);
    
    /**
     * Generate a complete response from AI model
     */
    Mono<String> generate(List<Message> messages, ModelConfig config);
    
    /**
     * Validate model configuration
     */
    boolean validateConfig(ModelConfig config);
}
