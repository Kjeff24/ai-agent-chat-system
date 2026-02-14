package com.aiagent.chatsystem.config;

import com.aiagent.chatsystem.dto.RegisterModelRequest;
import com.aiagent.chatsystem.model.DefaultProviderSetting;
import com.aiagent.chatsystem.model.DynamicProviderRegistration;
import com.aiagent.chatsystem.repository.DefaultProviderSettingRepository;
import com.aiagent.chatsystem.repository.DynamicProviderRegistrationRepository;
import com.aiagent.chatsystem.service.ModelFactory;
import com.aiagent.chatsystem.service.ProviderMetadata;
import com.aiagent.chatsystem.service.ModelRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

/**
 * Registers available AI models in the ModelRegistry at startup.
 * First registers static providers (from config), then loads persisted dynamic providers from DB.
 */
@Configuration
public class AIModelInitializer {

    /**
     * Register static and persisted dynamic providers at startup.
     */
    @Bean
    public CommandLineRunner modelRegistryInitializer(
            ModelRegistry modelRegistry,
            ModelFactory modelFactory,
            DynamicProviderRegistrationRepository persistedProviderRepository,
            DefaultProviderSettingRepository defaultProviderSettingRepository,
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

            List<DynamicProviderRegistration> persisted = persistedProviderRepository.findAllByOrderByCreatedAtAsc();
            for (DynamicProviderRegistration p : persisted) {
                try {
                    RegisterModelRequest req = new RegisterModelRequest();
                    req.setProvider(p.getProviderKey());
                    req.setType(p.getType());
                    req.setApiKey(p.getApiKey());
                    req.setBaseUrl(p.getBaseUrl());
                    req.setModels(p.getModels());
                    req.setDefaultModel(p.getDefaultModel());
                    var chatModel = modelFactory.build(req);
                    List<String> models = p.getModels() != null && !p.getModels().isEmpty()
                            ? p.getModels()
                            : (p.getDefaultModel() != null ? List.of(p.getDefaultModel()) : List.of());
                    ProviderMetadata metadata = ProviderMetadata.of(models, p.getDefaultModel(), p.getType());
                    modelRegistry.registerDynamicModel(p.getProviderKey(), chatModel, metadata);
                    System.out.println("✓ Dynamic provider restored: " + p.getProviderKey());
                } catch (Exception e) {
                    System.err.println("⚠ Failed to restore dynamic provider " + p.getProviderKey() + ": " + e.getMessage());
                }
            }

            defaultProviderSettingRepository.findById(DefaultProviderSetting.DEFAULT_ID).ifPresent(setting -> {
                if (setting.getProviderKey() != null && !setting.getProviderKey().isBlank()) {
                    modelRegistry.setDefaultProviderKey(setting.getProviderKey());
                    System.out.println("✓ Default provider set to: " + setting.getProviderKey());
                }
            });

            // If we have providers but no default was ever set, set the first (by creation order) as default
            if (!persisted.isEmpty()) {
                boolean noDefault = defaultProviderSettingRepository.findById(DefaultProviderSetting.DEFAULT_ID)
                        .map(s -> s.getProviderKey() == null || s.getProviderKey().isBlank())
                        .orElse(true);
                if (noDefault) {
                    String firstKey = persisted.get(0).getProviderKey();
                    if (firstKey != null && !firstKey.isBlank()) {
                        DefaultProviderSetting setting = defaultProviderSettingRepository.findById(DefaultProviderSetting.DEFAULT_ID)
                                .orElseGet(() -> DefaultProviderSetting.create(null));
                        setting.setId(DefaultProviderSetting.DEFAULT_ID);
                        setting.setProviderKey(firstKey);
                        defaultProviderSettingRepository.save(setting);
                        modelRegistry.setDefaultProviderKey(firstKey);
                        System.out.println("✓ First provider set as default: " + firstKey);
                    }
                }
            }

            if (modelRegistry.getRegisteredProviders().isEmpty()) {
                System.out.println("⚠ No AI models registered. Configure API keys or Ollama base URL to enable AI features.");
            } else {
                System.out.println("✓ Registered AI providers: " + modelRegistry.getRegisteredProviders());
            }
        };
    }
}
