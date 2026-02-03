package com.aiagent.chatsystem.service;

/**
 * Resolves the effective system prompt: either a dynamic override (set via API)
 * or the static default loaded from the configured file (e.g. system-prompt.md).
 */
public interface SystemPromptService {

    /**
     * Returns the current effective system prompt. If an override has been set via
     * {@link #setSystemPromptOverride(String)}, that value is returned; otherwise
     * the content of the file at {@code app.chat.system-prompt-path} is returned.
     * If neither is available, returns an empty string.
     */
    String getEffectiveSystemPrompt();

    /**
     * Sets a dynamic override for the system prompt. Use null or empty to clear.
     * This override takes precedence over the file-based default.
     */
    void setSystemPromptOverride(String prompt);

    /**
     * Clears any dynamic override so that the file-based default is used again.
     */
    void clearSystemPromptOverride();
}
