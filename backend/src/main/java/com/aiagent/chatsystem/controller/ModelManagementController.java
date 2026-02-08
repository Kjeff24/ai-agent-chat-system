package com.aiagent.chatsystem.controller;

import com.aiagent.chatsystem.dto.DiscoverModelsRequest;
import com.aiagent.chatsystem.dto.RegisterModelRequest;
import com.aiagent.chatsystem.dto.UpdateProviderRequest;
import com.aiagent.chatsystem.service.ModelDiscoveryService;
import com.aiagent.chatsystem.service.ModelProviderManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing AI model providers dynamically.
 * Delegates all business logic to {@link ModelProviderManagementService}.
 */
@RestController
@RequestMapping("/api/models/registry")
@Tag(name = "Model registry", description = "List and dynamically register/unregister AI model providers (JWT required)")
public class ModelManagementController {

    private final ModelProviderManagementService modelProviderManagementService;
    private final ModelDiscoveryService modelDiscoveryService;

    public ModelManagementController(ModelProviderManagementService modelProviderManagementService,
                                     ModelDiscoveryService modelDiscoveryService) {
        this.modelProviderManagementService = modelProviderManagementService;
        this.modelDiscoveryService = modelDiscoveryService;
    }

    @GetMapping
    @Operation(summary = "List providers", description = "Get all registered providers with dynamic flag, and when available: models list and defaultModel")
    @ApiResponse(responseCode = "200", description = "Success. Body: { providers, count, providersWithMeta: [{ name, dynamic, models?, defaultModel? }] }")
    public Map<String, Object> getRegisteredModels() {
        return modelProviderManagementService.getRegisteredProvidersSummary();
    }

    @GetMapping("/{provider}")
    @Operation(summary = "Check provider", description = "Check if a provider is registered and whether it is dynamic. For dynamic providers, returns edit details: type, baseUrl, models, defaultModel, apiKeyMasked.")
    @ApiResponse(responseCode = "200", description = "Success. Body: { provider, registered, dynamic, type?, baseUrl?, models?, defaultModel?, apiKeyMasked? }")
    public Map<String, Object> checkProvider(
            @Parameter(description = "Provider name (e.g. openai, ollama, openrouter)") @PathVariable String provider) {
        return modelProviderManagementService.getProviderDetails(provider);
    }

    @PostMapping("/discover")
    @Operation(summary = "Discover models", description = "List available model IDs for a provider type (openai, ollama, anthropic, bedrock). For openai/ollama requires baseUrl and optionally apiKey for openai. Returns curated list for anthropic/bedrock.")
    @ApiResponse(responseCode = "200", description = "Success. Body: { models: string[] }")
    public Map<String, List<String>> discoverModels(@RequestBody DiscoverModelsRequest request) {
        List<String> models = modelDiscoveryService.discoverModels(request);
        return Map.of("models", models);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register provider", description = "Register a new AI model provider at runtime. Provide a list of models and optionally defaultModel (must be in models); if only defaultModel is set, it is the only model; if only models is set, the first is used as default.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registered"),
            @ApiResponse(responseCode = "400", description = "Invalid request (e.g. missing API key, or defaultModel not in models)")
    })
    public Map<String, String> registerModel(@Valid @RequestBody RegisterModelRequest request) {
        return modelProviderManagementService.registerProvider(request);
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
        return modelProviderManagementService.updateProvider(provider, request);
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
        modelProviderManagementService.unregisterProvider(provider);
    }
}
