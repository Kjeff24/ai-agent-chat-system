package com.aiagent.chatsystem.service.impl;

import com.aiagent.chatsystem.dto.RegisterModelRequest;
import com.aiagent.chatsystem.dto.UpdateProviderRequest;
import com.aiagent.chatsystem.exception.InvalidModelConfigException;
import com.aiagent.chatsystem.exception.ModelProviderNotFoundException;
import com.aiagent.chatsystem.model.DynamicProviderRegistration;
import com.aiagent.chatsystem.repository.DynamicProviderRegistrationRepository;
import com.aiagent.chatsystem.service.ModelFactory;
import com.aiagent.chatsystem.service.ModelProviderManagementService;
import com.aiagent.chatsystem.service.ModelRegistry;
import com.aiagent.chatsystem.service.ProviderMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of model provider management: list, get details, register, update, unregister.
 * All business logic for dynamic provider CRUD lives here.
 */
@Service
public class ModelProviderManagementServiceImpl implements ModelProviderManagementService {

    private final ModelRegistry modelRegistry;
    private final ModelFactory modelFactory;
    private final DynamicProviderRegistrationRepository persistedProviderRepository;

    public ModelProviderManagementServiceImpl(ModelRegistry modelRegistry,
                                              ModelFactory modelFactory,
                                              DynamicProviderRegistrationRepository persistedProviderRepository) {
        this.modelRegistry = modelRegistry;
        this.modelFactory = modelFactory;
        this.persistedProviderRepository = persistedProviderRepository;
    }

    @Override
    public Map<String, Object> getRegisteredProvidersSummary() {
        Set<String> providers = modelRegistry.getRegisteredProviders();
        List<Map<String, Object>> withMeta = providers.stream()
                .map(name -> {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("name", name);
                    meta.put("dynamic", modelRegistry.isDynamic(name));
                    ProviderMetadata pm = modelRegistry.getProviderMetadata(name);
                    if (pm != null) {
                        meta.put("models", pm.models());
                        meta.put("defaultModel", pm.defaultModel());
                    }
                    return meta;
                })
                .collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("providers", providers);
        response.put("count", providers.size());
        response.put("providersWithMeta", withMeta);
        return response;
    }

    @Override
    public Map<String, Object> getProviderDetails(String provider) {
        String key = provider.trim().toLowerCase();
        boolean registered = modelRegistry.hasModel(key);
        boolean dynamic = modelRegistry.isDynamic(key);
        Map<String, Object> response = new HashMap<>();
        response.put("provider", key);
        response.put("registered", registered);
        response.put("dynamic", dynamic);
        if (dynamic) {
            persistedProviderRepository.findByProviderKeyIgnoreCase(key).ifPresent(p -> {
                response.put("type", p.getType());
                response.put("baseUrl", p.getBaseUrl());
                response.put("models", p.getModels());
                response.put("defaultModel", p.getDefaultModel());
                response.put("apiKeyMasked", (p.getApiKey() != null && !p.getApiKey().isBlank()) ? "••••••••" : null);
                response.put("secretKeyMasked", (p.getSecretKey() != null && !p.getSecretKey().isBlank()) ? "••••••••" : null);
            });
        }
        return response;
    }

    @Override
    @Transactional
    public Map<String, String> registerProvider(RegisterModelRequest request) {
        try {
            ChatModel chatModel = modelFactory.build(request);
            String provider = request.getProvider().trim().toLowerCase();
            String typeDefault = typeDefaultModel(request.getType());
            String defaultModel = modelFactory.resolveDefaultModel(request, typeDefault);
            List<String> models = request.getModels() != null && !request.getModels().isEmpty()
                    ? request.getModels()
                    : (defaultModel != null ? List.of(defaultModel) : List.of());
            ProviderMetadata metadata = ProviderMetadata.of(models, defaultModel, request.getType());
            modelRegistry.registerDynamicModel(provider, chatModel, metadata);

            DynamicProviderRegistration persisted = persistedProviderRepository.findByProviderKeyIgnoreCase(provider)
                    .orElse(new DynamicProviderRegistration());
            persisted.setProviderKey(provider);
            persisted.setType(request.getType() != null ? request.getType().trim().toLowerCase() : "openai");
            persisted.setApiKey(request.getApiKey());
            persisted.setSecretKey(request.getSecretKey());
            persisted.setBaseUrl(request.getBaseUrl());
            persisted.setModels(models);
            persisted.setDefaultModel(defaultModel);
            persistedProviderRepository.save(persisted);

            return Map.of("provider", provider, "status", "registered", "defaultModel", defaultModel != null ? defaultModel : "");
        } catch (IllegalArgumentException e) {
            throw new InvalidModelConfigException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, String> updateProvider(String provider, UpdateProviderRequest request) {
        String key = provider.trim().toLowerCase();
        DynamicProviderRegistration persisted = persistedProviderRepository.findByProviderKeyIgnoreCase(key)
                .orElseThrow(() -> new ModelProviderNotFoundException(
                        "Provider not found or not dynamic: " + provider + ". Only providers added via POST /api/models/registry can be updated."));
        if (!modelRegistry.isDynamic(key)) {
            throw new ModelProviderNotFoundException("Provider is not dynamic: " + provider);
        }

        if (request.getApiKey() != null) persisted.setApiKey(request.getApiKey());
        if (request.getSecretKey() != null) persisted.setSecretKey(request.getSecretKey());
        if (request.getBaseUrl() != null) persisted.setBaseUrl(request.getBaseUrl());
        if (request.getModels() != null) persisted.setModels(request.getModels());
        if (request.getDefaultModel() != null) persisted.setDefaultModel(request.getDefaultModel());

        List<String> modelsList = persisted.getModels();
        String defaultModelVal = persisted.getDefaultModel();
        if (modelsList != null && !modelsList.isEmpty() && defaultModelVal != null && !defaultModelVal.isBlank()
                && modelsList.stream().noneMatch(m -> defaultModelVal.equalsIgnoreCase(m != null ? m.trim() : ""))) {
            throw new InvalidModelConfigException("defaultModel must be one of models. defaultModel='" + defaultModelVal + "', models=" + modelsList);
        }

        RegisterModelRequest buildReq = new RegisterModelRequest();
        buildReq.setProvider(persisted.getProviderKey());
        buildReq.setType(persisted.getType());
        buildReq.setApiKey(persisted.getApiKey());
        buildReq.setSecretKey(persisted.getSecretKey());
        buildReq.setBaseUrl(persisted.getBaseUrl());
        buildReq.setModels(persisted.getModels());
        buildReq.setDefaultModel(persisted.getDefaultModel());

        try {
            ChatModel chatModel = modelFactory.build(buildReq);
            String typeDefault = typeDefaultModel(persisted.getType());
            String defaultModel = modelFactory.resolveDefaultModel(buildReq, typeDefault);
            List<String> models = persisted.getModels() != null && !persisted.getModels().isEmpty()
                    ? persisted.getModels()
                    : (defaultModel != null ? List.of(defaultModel) : List.of());
            ProviderMetadata metadata = ProviderMetadata.of(models, defaultModel, persisted.getType());
            modelRegistry.registerDynamicModel(key, chatModel, metadata);
            persisted.setModels(models);
            persisted.setDefaultModel(defaultModel);
            persistedProviderRepository.save(persisted);
            return Map.of("provider", key, "status", "updated", "defaultModel", defaultModel != null ? defaultModel : "");
        } catch (IllegalArgumentException e) {
            throw new InvalidModelConfigException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public void unregisterProvider(String provider) {
        if (!modelRegistry.unregisterModel(provider)) {
            throw new ModelProviderNotFoundException(
                    "Provider not found or not dynamic: " + provider + ". Only providers added via POST /api/models/registry can be removed.");
        }
        persistedProviderRepository.deleteByProviderKeyIgnoreCase(provider);
    }

    private static String typeDefaultModel(String type) {
        if (type == null) return "gpt-4";
        switch (type.trim().toLowerCase()) {
            case "openai": return "gpt-4";
            case "anthropic": return "claude-3-5-sonnet-latest";
            case "ollama": return "llama2";
            case "bedrock": return "anthropic.claude-3-5-sonnet-20240620-v1:0";
            default: return "gpt-4";
        }
    }
}
