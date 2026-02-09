package com.aiagent.chatsystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Persisted system prompt override (set via PUT /api/settings/system-prompt).
 * Single row with id = DEFAULT_ID; when absent or content empty, file default is used.
 */
@Entity
@Table(name = "system_prompt_override")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemPromptOverride {

    public static final String DEFAULT_ID = "default";

    @Id
    @Column(nullable = false, unique = true, length = 64)
    private String id = DEFAULT_ID;

    @Column(columnDefinition = "text")
    private String content;

    public static SystemPromptOverride create(String content) {
        SystemPromptOverride o = new SystemPromptOverride();
        o.setId(DEFAULT_ID);
        o.setContent(content);
        return o;
    }
}
