package com.aiagent.chatsystem.controller;

import com.aiagent.chatsystem.service.ModelRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * REST controller for managing AI models dynamically.
 * Allows adding/removing models at runtime.
 */
@RestController
@RequestMapping("/api/models/registry")
@Tag(name = "Model registry", description = "Inspect registered AI model providers (JWT required)")
public class ModelManagementController {

    private final ModelRegistry modelRegistry;

    public ModelManagementController(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    @GetMapping
    @Operation(summary = "List providers", description = "Get all registered model providers (e.g. openai, anthropic, ollama)")
    @ApiResponse(responseCode = "200", description = "Success. Body: { providers: string[], count: number }")
    public Map<String, Object> getRegisteredModels() {
        Set<String> providers = modelRegistry.getRegisteredProviders();
        Map<String, Object> response = new HashMap<>();
        response.put("providers", providers);
        response.put("count", providers.size());
        return response;
    }

    @GetMapping("/{provider}")
    @Operation(summary = "Check provider", description = "Check if a specific provider is registered")
    @ApiResponse(responseCode = "200", description = "Success. Body: { provider: string, registered: boolean }")
    public Map<String, Object> checkProvider(
            @Parameter(description = "Provider name (e.g. openai, ollama)") @PathVariable String provider) {
        boolean registered = modelRegistry.hasModel(provider);
        Map<String, Object> response = new HashMap<>();
        response.put("provider", provider);
        response.put("registered", registered);
        return response;
    }

    @PostMapping("/{provider}")
    @Operation(summary = "Register model", description = "Not implemented. Configure via application.yml or env.")
    @ApiResponse(responseCode = "400", description = "Dynamic registration not supported")
    public Map<String, String> registerModel(
            @Parameter(description = "Provider name") @PathVariable String provider,
            @RequestBody Map<String, String> request) {
        throw new UnsupportedOperationException(
                "Dynamic model registration via API not yet implemented. " +
                        "Configure models via application.yml or environment variables.");
    }
}
