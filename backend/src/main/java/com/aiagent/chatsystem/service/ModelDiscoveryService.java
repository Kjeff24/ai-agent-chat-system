package com.aiagent.chatsystem.service;

import com.aiagent.chatsystem.dto.DiscoverModelsRequest;

import java.util.List;

/**
 * Discovers available model IDs from a provider (OpenAI, Ollama, Anthropic, Bedrock).
 */
public interface ModelDiscoveryService {

    /**
     * List model IDs for the given provider type and optional credentials.
     *
     * @param request type, and optionally baseUrl, apiKey, secretKey (bedrock)
     * @return list of model IDs (e.g. gpt-4, llama2, anthropic.claude-3-5-sonnet-...)
     * @throws IllegalArgumentException if type is unsupported or request is invalid
     */
    List<String> discoverModels(DiscoverModelsRequest request);
}
