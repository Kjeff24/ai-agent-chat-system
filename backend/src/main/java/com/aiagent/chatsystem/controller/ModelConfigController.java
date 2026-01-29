package com.aiagent.chatsystem.controller;

import com.aiagent.chatsystem.dto.CreateModelConfigRequest;
import com.aiagent.chatsystem.model.ModelConfig;
import com.aiagent.chatsystem.service.ModelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/models")
@Tag(name = "Model configs", description = "CRUD for AI model configurations (OpenAI, Anthropic, Ollama, etc.) (JWT required)")
public class ModelConfigController {

    private final ModelConfigService modelConfigService;

    public ModelConfigController(ModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    @GetMapping
    @Operation(summary = "List configs", description = "Get all model configurations")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ModelConfig.class))))
    public List<ModelConfig> getAllConfigs() {
        return modelConfigService.getAllConfigs();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get config", description = "Get a model configuration by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = ModelConfig.class))),
            @ApiResponse(responseCode = "404", description = "Config not found")
    })
    public ModelConfig getConfig(@Parameter(description = "Config UUID") @PathVariable UUID id) {
        return modelConfigService.getConfig(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create config", description = "Create a new model configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = ModelConfig.class))),
            @ApiResponse(responseCode = "400", description = "Invalid config")
    })
    public ModelConfig createConfig(@Valid @RequestBody CreateModelConfigRequest request) {
        return modelConfigService.createConfig(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update config", description = "Partially update a model configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = ModelConfig.class))),
            @ApiResponse(responseCode = "404", description = "Config not found"),
            @ApiResponse(responseCode = "400", description = "Invalid updates")
    })
    public ModelConfig updateConfig(
            @Parameter(description = "Config UUID") @PathVariable UUID id,
            @RequestBody Map<String, Object> updates) {
        return modelConfigService.updateConfig(id, updates);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete config", description = "Soft-delete a model configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Config not found")
    })
    public void deleteConfig(@Parameter(description = "Config UUID") @PathVariable UUID id) {
        modelConfigService.deleteConfig(id);
    }
}
