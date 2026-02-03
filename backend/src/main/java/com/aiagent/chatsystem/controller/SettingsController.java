package com.aiagent.chatsystem.controller;

import com.aiagent.chatsystem.dto.SystemPromptValueDTO;
import com.aiagent.chatsystem.service.SystemPromptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * Settings API: system prompt (and future settings). JWT required via security config.
 */
@RestController
@RequestMapping("/api/settings")
@Tag(name = "Settings", description = "Application settings such as system prompt (JWT required)")
public class SettingsController {

    private final SystemPromptService systemPromptService;

    public SettingsController(SystemPromptService systemPromptService) {
        this.systemPromptService = systemPromptService;
    }

    @GetMapping(value = "/system-prompt", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get system prompt", description = "Returns the current effective system prompt (override or file default)")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = SystemPromptValueDTO.class)))
    public SystemPromptValueDTO getSystemPrompt() {
        String effective = systemPromptService.getEffectiveSystemPrompt();
        return new SystemPromptValueDTO(effective != null ? effective : "");
    }

    @PutMapping(value = "/system-prompt", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Set system prompt override", description = "Set or clear the dynamic system prompt override. Send { \"value\": \"...\" }; omit or empty value clears the override.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = SystemPromptValueDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public SystemPromptValueDTO setSystemPrompt(@RequestBody(required = false) SystemPromptValueDTO body) {
        String value = (body != null && body.getValue() != null) ? body.getValue().trim() : "";
        if (value.isEmpty()) {
            systemPromptService.clearSystemPromptOverride();
            return new SystemPromptValueDTO(systemPromptService.getEffectiveSystemPrompt());
        }
        systemPromptService.setSystemPromptOverride(value);
        return new SystemPromptValueDTO(value);
    }
}
