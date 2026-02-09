package com.aiagent.chatsystem.service.impl;

import com.aiagent.chatsystem.config.AppChatProperties;
import com.aiagent.chatsystem.model.SystemPromptOverride;
import com.aiagent.chatsystem.repository.SystemPromptOverrideRepository;
import com.aiagent.chatsystem.service.SystemPromptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

/**
 * Loads the default system prompt from the configured file (classpath: or file:)
 * and supports a persisted override set via the settings API (survives restarts).
 */
@Service
public class SystemPromptServiceImpl implements SystemPromptService {

    private static final Logger logger = LoggerFactory.getLogger(SystemPromptServiceImpl.class);

    private final AppChatProperties appChatProperties;
    private final ResourceLoader resourceLoader;
    private final SystemPromptOverrideRepository systemPromptOverrideRepository;
    private volatile String cachedFileContent;
    private volatile boolean fileLoadAttempted;

    public SystemPromptServiceImpl(AppChatProperties appChatProperties, ResourceLoader resourceLoader,
                                   SystemPromptOverrideRepository systemPromptOverrideRepository) {
        this.appChatProperties = appChatProperties;
        this.resourceLoader = resourceLoader;
        this.systemPromptOverrideRepository = systemPromptOverrideRepository;
    }

    @Override
    public String getEffectiveSystemPrompt() {
        String over = systemPromptOverrideRepository.findById(SystemPromptOverride.DEFAULT_ID)
                .map(SystemPromptOverride::getContent)
                .orElse(null);
        if (over != null && !over.isEmpty()) {
            return over;
        }
        return loadDefaultFromFile();
    }

    @Override
    @Transactional
    public void setSystemPromptOverride(String prompt) {
        String trimmed = prompt == null ? null : prompt.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            systemPromptOverrideRepository.findById(SystemPromptOverride.DEFAULT_ID)
                    .ifPresent(systemPromptOverrideRepository::delete);
            logger.debug("System prompt override cleared");
        } else {
            SystemPromptOverride entity = systemPromptOverrideRepository.findById(SystemPromptOverride.DEFAULT_ID)
                    .orElseGet(() -> SystemPromptOverride.create(null));
            entity.setContent(trimmed);
            systemPromptOverrideRepository.save(entity);
            logger.debug("System prompt override set (length={})", trimmed.length());
        }
    }

    @Override
    @Transactional
    public void clearSystemPromptOverride() {
        systemPromptOverrideRepository.findById(SystemPromptOverride.DEFAULT_ID)
                .ifPresent(systemPromptOverrideRepository::delete);
        logger.debug("System prompt override cleared");
    }

    private String loadDefaultFromFile() {
        if (fileLoadAttempted) {
            return cachedFileContent != null ? cachedFileContent : "";
        }
        synchronized (this) {
            if (fileLoadAttempted) {
                return cachedFileContent != null ? cachedFileContent : "";
            }
            String path = appChatProperties.getSystemPromptPath();
            if (path == null || path.isEmpty()) {
                fileLoadAttempted = true;
                return "";
            }
            try {
                Resource resource = resourceLoader.getResource(path);
                if (!resource.exists()) {
                    logger.warn("System prompt resource does not exist: {}", path);
                    cachedFileContent = "";
                } else {
                    cachedFileContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                    if (cachedFileContent != null) {
                        cachedFileContent = cachedFileContent.trim();
                    } else {
                        cachedFileContent = "";
                    }
                    logger.debug("Loaded system prompt from {} (length={})", path, cachedFileContent.length());
                }
            } catch (Exception e) {
                logger.warn("Failed to load system prompt from {}: {}", path, e.getMessage());
                cachedFileContent = "";
            }
            fileLoadAttempted = true;
            return cachedFileContent != null ? cachedFileContent : "";
        }
    }
}
