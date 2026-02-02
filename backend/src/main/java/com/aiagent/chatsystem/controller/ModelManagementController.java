package com.aiagent.chatsystem.controller;

import com.aiagent.chatsystem.dto.RegisterModelRequest;
import com.aiagent.chatsystem.dto.UpdateProviderRequest;
import com.aiagent.chatsystem.exception.InvalidModelConfigException;
import com.aiagent.chatsystem.exception.ModelProviderNotFoundException;
import com.aiagent.chatsystem.model.DynamicProviderRegistration;
import com.aiagent.chatsystem.repository.DynamicProviderRegistrationRepository;
import com.aiagent.chatsystem.service.ModelFactory;
import com.aiagent.chatsystem.service.ProviderMetadata;
import com.aiagent.chatsystem.service.ModelRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller for managing AI model providers dynamically.
 * List providers, register new ones (OpenAI-compatible or Ollama), unregister dynamic ones.
 */
@RestController
@RequestMapping("/api/models/registry")
@Tag(name = "Model registry", description = "List and dynamically register/unregister AI model providers (JWT required)")
public class ModelManagementController {

    private final ModelRegistry modelRegistry;
    private final ModelFactory modelFactory;
    private final DynamicProviderRegistrationRepository persistedProviderRepository;

    public ModelManagementController(ModelRegistry modelRegistry, ModelFactory modelFactory,
                                     DynamicProviderRegistrationRepository persistedProviderRepository) {
        this.modelRegistry = modelRegistry;
        this.modelFactory = modelFactory;
        this.persistedProviderRepository = persistedProviderRepository;
    }

    @GetMapping
    @Operation(summary = "List providers", description = "Get all registered providers with dynamic flag, and when available: models list and defaultModel")
    @ApiResponse(responseCode = "200", description = "Success. Body: { providers, count, providersWithMeta: [{ name, dynamic, models?, defaultModel? }] }")
    public Map<String, Object> getRegisteredModels() {
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

    @GetMapping("/{provider}")
    @Operation(summary = "Check provider", description = "Check if a provider is registered and whether it is dynamic. For dynamic providers, returns edit details: type, baseUrl, models, defaultModel, apiKeyMasked.")
    @ApiResponse(responseCode = "200", description = "Success. Body: { provider, registered, dynamic, type?, baseUrl?, models?, defaultModel?, apiKeyMasked? }")
    public Map<String, Object> checkProvider(
            @Parameter(description = "Provider name (e.g. openai, ollama, openrouter)") @PathVariable String provider) {
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
            });
        }
        return response;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register provider", description = "Register a new AI model provider at runtime. Provide a list of models and optionally defaultModel (must be in models); if only defaultModel is set, it is the only model; if only models is set, the first is used as default.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registered"),
            @ApiResponse(responseCode = "400", description = "Invalid request (e.g. missing API key, or defaultModel not in models)")
    })
    public Map<String, String> registerModel(@Valid @RequestBody RegisterModelRequest request) {
        try {
            var chatModel = modelFactory.build(request);
            String provider = request.getProvider().trim().toLowerCase();
            String typeDefault = typeDefaultModel(request.getType());
            String defaultModel = modelFactory.resolveDefaultModel(request, typeDefault);
            List<String> models = request.getModels() != null && !request.getModels().isEmpty()
                    ? request.getModels()
                    : (defaultModel != null ? List.of(defaultModel) : List.of());
            ProviderMetadata metadata = ProviderMetadata.of(models, defaultModel);
            modelRegistry.registerDynamicModel(provider, chatModel, metadata);

            DynamicProviderRegistration persisted = persistedProviderRepository.findByProviderKeyIgnoreCase(provider)
                    .orElse(new DynamicProviderRegistration());
            persisted.setProviderKey(provider);
            persisted.setType(request.getType() != null ? request.getType().trim().toLowerCase() : "openai");
            persisted.setApiKey(request.getApiKey());
            persisted.setBaseUrl(request.getBaseUrl());
            persisted.setModels(models);
            persisted.setDefaultModel(defaultModel);
            persistedProviderRepository.save(persisted);

            return Map.of("provider", provider, "status", "registered", "defaultModel", defaultModel != null ? defaultModel : "");
        } catch (IllegalArgumentException e) {
            throw new InvalidModelConfigException(e.getMessage());
        }
    }

    @RequestMapping(value = "/{provider}", method = { RequestMethod.PATCH, RequestMethod.PUT })
    @Operation(summary = "Update provider", description = "Update a dynamically registered provider. Only dynamic providers can be updated. Omit fields to keep current values. Provider name and type are immutable.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated. Body: { provider, status, defaultModel }"),
            @ApiResponse(responseCode = "400", description = "Invalid request (e.g. defaultModel not in models)"),
            @ApiResponse(responseCode = "404", description = "Provider not found or not dynamic")
    })
    public Map<String, String> updateProvider(
            @Parameter(description = "Provider name to update") @PathVariable String provider,
            @RequestBody UpdateProviderRequest request) {
        String key = provider.trim().toLowerCase();
        DynamicProviderRegistration persisted = persistedProviderRepository.findByProviderKeyIgnoreCase(key)
                .orElseThrow(() -> new ModelProviderNotFoundException(
                        "Provider not found or not dynamic: " + provider + ". Only providers added via POST /api/models/registry can be updated."));
        if (!modelRegistry.isDynamic(key)) {
            throw new ModelProviderNotFoundException("Provider is not dynamic: " + provider);
        }

        if (request.getApiKey() != null) persisted.setApiKey(request.getApiKey());
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
        buildReq.setBaseUrl(persisted.getBaseUrl());
        buildReq.setModels(persisted.getModels());
        buildReq.setDefaultModel(persisted.getDefaultModel());

        try {
            var chatModel = modelFactory.build(buildReq);
            String typeDefault = typeDefaultModel(persisted.getType());
            String defaultModel = modelFactory.resolveDefaultModel(buildReq, typeDefault);
            List<String> models = persisted.getModels() != null && !persisted.getModels().isEmpty()
                    ? persisted.getModels()
                    : (defaultModel != null ? List.of(defaultModel) : List.of());
            ProviderMetadata metadata = ProviderMetadata.of(models, defaultModel);
            modelRegistry.registerDynamicModel(key, chatModel, metadata);
            persisted.setModels(models);
            persisted.setDefaultModel(defaultModel);
            persistedProviderRepository.save(persisted);
            return Map.of("provider", key, "status", "updated", "defaultModel", defaultModel != null ? defaultModel : "");
        } catch (IllegalArgumentException e) {
            throw new InvalidModelConfigException(e.getMessage());
        }
    }

    private static String typeDefaultModel(String type) {
        if (type == null) return "gpt-4";
        switch (type.trim().toLowerCase()) {
            case "openai": return "gpt-4";
            case "anthropic": return "claude-3-5-sonnet-latest";
            case "ollama": return "llama2";
            default: return "gpt-4";
        }
    }

    @DeleteMapping("/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unregister provider", description = "Remove a dynamically registered provider. Fails if provider was not added via API.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Removed"),
            @ApiResponse(responseCode = "404", description = "Not found or not dynamic (cannot remove static providers)")
    })
    public void unregisterModel(
            @Parameter(description = "Provider name to remove") @PathVariable String provider) {
        if (!modelRegistry.unregisterModel(provider)) {
            throw new ModelProviderNotFoundException(
                    "Provider not found or not dynamic: " + provider + ". Only providers added via POST /api/models/registry can be removed.");
        }
        persistedProviderRepository.deleteByProviderKeyIgnoreCase(provider);
    }
}
