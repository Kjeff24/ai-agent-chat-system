package com.aiagent.chatsystem.service.impl;

import com.aiagent.chatsystem.config.AppMcpOAuthProperties;
import com.aiagent.chatsystem.model.McpOAuthProvider;
import com.aiagent.chatsystem.repository.McpOAuthProviderRepository;
import com.aiagent.chatsystem.service.McpOAuthProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class McpOAuthProviderRegistryImpl implements McpOAuthProviderRegistry {

    private static final Logger logger = LoggerFactory.getLogger(McpOAuthProviderRegistryImpl.class);

    private final AppMcpOAuthProperties oauthProperties;
    private final McpOAuthProviderRepository mcpOAuthProviderRepository;
    private final Map<String, AppMcpOAuthProperties.OAuthProviderConfig> dynamicProviders = new ConcurrentHashMap<>();

    public McpOAuthProviderRegistryImpl(AppMcpOAuthProperties oauthProperties,
                                        McpOAuthProviderRepository mcpOAuthProviderRepository) {
        this.oauthProperties = oauthProperties;
        this.mcpOAuthProviderRepository = mcpOAuthProviderRepository;
    }

    @PostConstruct
    public void loadDynamicProvidersFromDb() {
        for (McpOAuthProvider entity : mcpOAuthProviderRepository.findAllByOrderByCreatedAtAsc()) {
            AppMcpOAuthProperties.OAuthProviderConfig config = entityToConfig(entity);
            dynamicProviders.put(entity.getProviderId(), config);
        }
        if (!dynamicProviders.isEmpty()) {
            logger.info("Loaded {} OAuth provider(s) from database", dynamicProviders.size());
        }
    }

    private static AppMcpOAuthProperties.OAuthProviderConfig entityToConfig(McpOAuthProvider entity) {
        AppMcpOAuthProperties.OAuthProviderConfig config = new AppMcpOAuthProperties.OAuthProviderConfig();
        config.setAuthorizationUri(entity.getAuthorizationUri());
        config.setTokenUri(entity.getTokenUri());
        config.setClientId(entity.getClientId());
        config.setClientSecret(entity.getClientSecret());
        config.setScopes(entity.getScopes());
        return config;
    }

    private static McpOAuthProvider configToEntity(String providerId, AppMcpOAuthProperties.OAuthProviderConfig config) {
        McpOAuthProvider entity = new McpOAuthProvider();
        entity.setProviderId(providerId);
        entity.setAuthorizationUri(config.getAuthorizationUri());
        entity.setTokenUri(config.getTokenUri());
        entity.setClientId(config.getClientId());
        entity.setClientSecret(config.getClientSecret());
        entity.setScopes(config.getScopes());
        return entity;
    }

    @Override
    public Map<String, AppMcpOAuthProperties.OAuthProviderConfig> getEffectiveProviders() {
        Map<String, AppMcpOAuthProperties.OAuthProviderConfig> out = new LinkedHashMap<>();
        Map<String, AppMcpOAuthProperties.OAuthProviderConfig> staticProviders = oauthProperties.getProviders();
        if (staticProviders != null) {
            out.putAll(staticProviders);
        }
        out.putAll(dynamicProviders);
        return out;
    }

    @Override
    public AppMcpOAuthProperties.OAuthProviderConfig getProvider(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return null;
        }
        AppMcpOAuthProperties.OAuthProviderConfig dynamic = dynamicProviders.get(providerId);
        if (dynamic != null) {
            return dynamic;
        }
        Map<String, AppMcpOAuthProperties.OAuthProviderConfig> staticProviders = oauthProperties.getProviders();
        return staticProviders != null ? staticProviders.get(providerId) : null;
    }

    @Override
    public String getSource(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return null;
        }
        return dynamicProviders.containsKey(providerId) ? "dynamic" : "static";
    }

    @Override
    @Transactional
    public void addProvider(String providerId, AppMcpOAuthProperties.OAuthProviderConfig config) {
        if (providerId == null || providerId.isBlank() || config == null) return;
        String id = providerId.trim();
        McpOAuthProvider entity = configToEntity(id, config);
        mcpOAuthProviderRepository.save(entity);
        dynamicProviders.put(id, copyConfig(config));
    }

    @Override
    @Transactional
    public void updateProvider(String providerId, AppMcpOAuthProperties.OAuthProviderConfig config) {
        if (providerId == null || providerId.isBlank() || config == null) return;
        String id = providerId.trim();
        McpOAuthProvider entity = mcpOAuthProviderRepository.findByProviderId(id).orElse(null);
        if (entity == null) return;
        entity.setAuthorizationUri(config.getAuthorizationUri());
        entity.setTokenUri(config.getTokenUri());
        entity.setClientId(config.getClientId());
        entity.setClientSecret(config.getClientSecret());
        entity.setScopes(config.getScopes());
        mcpOAuthProviderRepository.save(entity);
        dynamicProviders.put(id, copyConfig(config));
    }

    @Override
    @Transactional
    public boolean removeProvider(String providerId) {
        if (providerId == null || providerId.isBlank()) return false;
        String id = providerId.trim();
        if (!mcpOAuthProviderRepository.existsByProviderId(id)) return false;
        mcpOAuthProviderRepository.deleteByProviderId(id);
        dynamicProviders.remove(id);
        return true;
    }

    private static AppMcpOAuthProperties.OAuthProviderConfig copyConfig(AppMcpOAuthProperties.OAuthProviderConfig src) {
        AppMcpOAuthProperties.OAuthProviderConfig c = new AppMcpOAuthProperties.OAuthProviderConfig();
        c.setAuthorizationUri(src.getAuthorizationUri());
        c.setTokenUri(src.getTokenUri());
        c.setClientId(src.getClientId());
        c.setClientSecret(src.getClientSecret());
        c.setScopes(src.getScopes());
        return c;
    }
}
