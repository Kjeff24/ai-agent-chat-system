package com.aiagent.chatsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Chat-related configuration. Binds to {@code app.chat} in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "app.chat")
public class AppChatProperties {

    /**
     * Path to system prompt file (classpath: or file:). Empty = no file default.
     */
    private String systemPromptPath = "";

    public String getSystemPromptPath() {
        return systemPromptPath != null ? systemPromptPath : "";
    }

    public void setSystemPromptPath(String systemPromptPath) {
        this.systemPromptPath = systemPromptPath != null ? systemPromptPath : "";
    }
}
