package com.aiagent.chatsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request/response body for system prompt API: current or new value.
 */
@Schema(description = "System prompt value (get current or set override)")
public class SystemPromptValueDTO {

    @Schema(description = "The system prompt text. Omit or empty to clear override.", example = "You are a helpful assistant.")
    private String value;

    public SystemPromptValueDTO() {
    }

    public SystemPromptValueDTO(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
