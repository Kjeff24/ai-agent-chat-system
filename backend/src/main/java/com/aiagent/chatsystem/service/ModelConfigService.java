package com.aiagent.chatsystem.service;

import com.aiagent.chatsystem.dto.CreateModelConfigRequest;
import com.aiagent.chatsystem.model.ModelConfig;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ModelConfigService {

    List<ModelConfig> getAllConfigs();

    ModelConfig getConfig(UUID id);

    ModelConfig createConfig(CreateModelConfigRequest request);

    ModelConfig updateConfig(UUID id, Map<String, Object> updates);

    void deleteConfig(UUID id);
}
