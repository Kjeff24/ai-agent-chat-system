package com.aiagent.chatsystem.config;

import com.aiagent.chatsystem.model.ModelConfig;
import com.aiagent.chatsystem.repository.ModelConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Seeds a default Ollama model configuration on startup when none exists.
 * This ensures "Create conversation" works out of the box when using Ollama.
 */
@Component
@Order(100)
public class DefaultModelConfigInitializer implements CommandLineRunner {

    private final ModelConfigRepository modelConfigRepository;

    @Value("${spring.ai.ollama.chat.options.model:llama2}")
    private String ollamaModel;

    public DefaultModelConfigInitializer(ModelConfigRepository modelConfigRepository) {
        this.modelConfigRepository = modelConfigRepository;
    }

    @Override
    public void run(String... args) {
        if (modelConfigRepository.findByIsDefaultTrueAndIsActiveTrue().isPresent()) {
            return;
        }

        ModelConfig config = new ModelConfig();
        config.setName("Ollama (Default)");
        config.setProvider(ModelConfig.ModelProvider.ollama);
        config.setModel(ollamaModel);
        config.setParameters(Map.of("temperature", 0.7, "maxTokens", 2048));
        config.setIsDefault(true);
        config.setIsActive(true);

        modelConfigRepository.save(config);
        System.out.println("✓ Seeded default model config: Ollama (" + ollamaModel + ")");
    }
}
