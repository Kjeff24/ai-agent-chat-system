package com.aiagent.chatsystem.config;

import com.aiagent.chatsystem.service.ModelRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * Registers available AI models dynamically in the ModelRegistry.
 * Models are only registered if they are available (configured with API keys/base URLs).
 */
@Configuration
public class AIModelInitializer {
    
    /**
     * Register available models in the registry at startup.
     * Models are injected only if they exist (conditional on configuration).
     */
    @Bean
    public CommandLineRunner modelRegistryInitializer(
            ModelRegistry modelRegistry,
            Optional<org.springframework.ai.openai.OpenAiChatModel> openAiChatModel,
            Optional<org.springframework.ai.anthropic.AnthropicChatModel> anthropicChatModel,
            Optional<org.springframework.ai.ollama.OllamaChatModel> ollamaChatModel) {
        return args -> {
            openAiChatModel.ifPresent(model -> {
                modelRegistry.registerModel("openai", model);
                System.out.println("✓ OpenAI model registered");
            });
            
            anthropicChatModel.ifPresent(model -> {
                modelRegistry.registerModel("anthropic", model);
                System.out.println("✓ Anthropic model registered");
            });
            
            ollamaChatModel.ifPresent(model -> {
                modelRegistry.registerModel("ollama", model);
                System.out.println("✓ Ollama model registered");
            });
            
            if (modelRegistry.getRegisteredProviders().isEmpty()) {
                System.out.println("⚠ No AI models registered. Configure API keys or Ollama base URL to enable AI features.");
            } else {
                System.out.println("✓ Registered AI providers: " + modelRegistry.getRegisteredProviders());
            }
        };
    }
}
