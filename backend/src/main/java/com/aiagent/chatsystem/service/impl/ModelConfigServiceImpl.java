package com.aiagent.chatsystem.service.impl;

import com.aiagent.chatsystem.dto.CreateModelConfigRequest;
import com.aiagent.chatsystem.exception.InvalidModelConfigException;
import com.aiagent.chatsystem.exception.ModelConfigNotFoundException;
import com.aiagent.chatsystem.model.ModelConfig;
import com.aiagent.chatsystem.repository.ModelConfigRepository;
import com.aiagent.chatsystem.service.AIModelService;
import com.aiagent.chatsystem.service.ModelConfigService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ModelConfigServiceImpl implements ModelConfigService {

    private final ModelConfigRepository modelConfigRepository;
    private final AIModelService aiModelService;

    public ModelConfigServiceImpl(
            ModelConfigRepository modelConfigRepository,
            AIModelService aiModelService) {
        this.modelConfigRepository = modelConfigRepository;
        this.aiModelService = aiModelService;
    }

    @Override
    public List<ModelConfig> getAllConfigs() {
        return modelConfigRepository.findActiveConfigs();
    }

    @Override
    public ModelConfig getConfig(UUID id) {
        return modelConfigRepository.findById(id)
                .orElseThrow(() -> new ModelConfigNotFoundException(id));
    }

    @Override
    public ModelConfig createConfig(CreateModelConfigRequest request) {
        ModelConfig config = toModelConfig(request);
        if (!aiModelService.validateConfig(config)) {
            throw new InvalidModelConfigException("Invalid model configuration");
        }
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unsetOtherDefaults();
        }
        return modelConfigRepository.save(config);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ModelConfig updateConfig(UUID id, Map<String, Object> updates) {
        ModelConfig config = modelConfigRepository.findById(id)
                .orElseThrow(() -> new ModelConfigNotFoundException(id));

        if (updates.containsKey("name")) {
            config.setName((String) updates.get("name"));
        }
        if (updates.containsKey("model")) {
            config.setModel((String) updates.get("model"));
        }
        if (updates.containsKey("parameters")) {
            config.setParameters((Map<String, Object>) updates.get("parameters"));
        }
        if (updates.containsKey("isDefault")) {
            Boolean isDefault = (Boolean) updates.get("isDefault");
            if (Boolean.TRUE.equals(isDefault)) {
                unsetOtherDefaults();
            }
            config.setIsDefault(isDefault);
        }
        if (updates.containsKey("isActive")) {
            config.setIsActive((Boolean) updates.get("isActive"));
        }

        ModelConfig saved = modelConfigRepository.save(config);
        if (!aiModelService.validateConfig(saved)) {
            throw new InvalidModelConfigException("Invalid model configuration after update");
        }
        return saved;
    }

    @Override
    public void deleteConfig(UUID id) {
        ModelConfig config = modelConfigRepository.findById(id)
                .orElseThrow(() -> new ModelConfigNotFoundException(id));
        config.setIsActive(false);
        modelConfigRepository.save(config);
    }

    private void unsetOtherDefaults() {
        modelConfigRepository.findByIsDefaultTrueAndIsActiveTrue().ifPresent(c -> {
            c.setIsDefault(false);
            modelConfigRepository.save(c);
        });
    }

    private ModelConfig toModelConfig(CreateModelConfigRequest request) {
        ModelConfig config = new ModelConfig();
        config.setName(request.getName());
        String providerStr = request.getProvider() != null ? request.getProvider().trim().toLowerCase() : "";
        ModelConfig.ModelProvider providerEnum = toProviderEnum(providerStr);
        config.setProvider(providerEnum);
        config.setModel(request.getModel());
        Map<String, Object> params = request.getParameters() != null ? new HashMap<>(request.getParameters()) : new HashMap<>();
        if (providerEnum == ModelConfig.ModelProvider.custom) {
            params.put("providerKey", providerStr);
        }
        config.setParameters(params);
        config.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        config.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return config;
    }

    /** Map request provider string to enum; dynamic providers (e.g. openrouter) become custom. */
    private static ModelConfig.ModelProvider toProviderEnum(String providerStr) {
        if (providerStr == null || providerStr.isEmpty()) return ModelConfig.ModelProvider.custom;
        try {
            return ModelConfig.ModelProvider.valueOf(providerStr);
        } catch (IllegalArgumentException e) {
            return ModelConfig.ModelProvider.custom;
        }
    }
}
