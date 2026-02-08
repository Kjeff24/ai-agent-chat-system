package com.aiagent.chatsystem.service.impl;

import com.aiagent.chatsystem.dto.DiscoverModelsRequest;
import com.aiagent.chatsystem.service.ModelDiscoveryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Discovers model IDs from OpenAI (GET /v1/models), Ollama (GET /api/tags),
 * and returns curated lists for Anthropic and Bedrock (no list API).
 */
@Service
public class ModelDiscoveryServiceImpl implements ModelDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(ModelDiscoveryServiceImpl.class);

    private static final String OPENAI_DEFAULT_BASE = "https://api.openai.com";
    private static final String OLLAMA_DEFAULT_BASE = "http://localhost:11434";

    private static final List<String> ANTHROPIC_MODELS = List.of(
            "claude-3-5-sonnet-20241022",
            "claude-3-5-sonnet-latest",
            "claude-3-opus-20240229",
            "claude-3-sonnet-20240229",
            "claude-3-haiku-20240307"
    );

    private static final List<String> BEDROCK_MODELS = List.of(
            "anthropic.claude-3-5-sonnet-20240620-v1:0",
            "anthropic.claude-3-5-haiku-20241022-v1:0",
            "anthropic.claude-3-sonnet-20240229-v1:0",
            "anthropic.claude-3-haiku-20240307-v1:0",
            "meta.llama3-70b-instruct-v1:0",
            "meta.llama3-8b-instruct-v1:0",
            "mistral.mistral-large-2402-v1:0",
            "amazon.nova-micro-v1:0",
            "amazon.nova-lite-v1:0"
    );

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<String> discoverModels(DiscoverModelsRequest request) {
        if (request == null || request.getType() == null || request.getType().isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        String type = request.getType().trim().toLowerCase();
        return switch (type) {
            case "openai" -> discoverOpenAI(request);
            case "ollama" -> discoverOllama(request);
            case "anthropic" -> discoverAnthropic();
            case "bedrock" -> discoverBedrock();
            default -> throw new IllegalArgumentException("Unsupported type for discovery: " + type + ". Use openai, ollama, anthropic, or bedrock.");
        };
    }

    private List<String> discoverOpenAI(DiscoverModelsRequest request) {
        String apiKey = request.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key is required for OpenAI discovery");
        }
        String base = request.getBaseUrl() != null && !request.getBaseUrl().isBlank()
                ? request.getBaseUrl().trim().replaceAll("/$", "")
                : OPENAI_DEFAULT_BASE;
        String url = base + "/v1/models";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        ResponseEntity<String> response = restTemplate.exchange(
                URI.create(url),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        return parseOpenAIModels(response.getBody());
    }

    private List<String> parseOpenAIModels(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) return Collections.emptyList();
            List<String> ids = new ArrayList<>();
            for (JsonNode node : data) {
                JsonNode id = node.get("id");
                if (id != null && id.isTextual()) {
                    String s = id.asText().trim();
                    if (!s.isEmpty()) ids.add(s);
                }
            }
            return ids;
        } catch (Exception e) {
            log.warn("Failed to parse OpenAI models response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> discoverOllama(DiscoverModelsRequest request) {
        String base = request.getBaseUrl() != null && !request.getBaseUrl().isBlank()
                ? request.getBaseUrl().trim().replaceAll("/$", "")
                : OLLAMA_DEFAULT_BASE;
        String url = base + "/api/tags";

        ResponseEntity<String> response = restTemplate.getForEntity(URI.create(url), String.class);
        return parseOllamaModels(response.getBody());
    }

    private List<String> parseOllamaModels(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode models = root.get("models");
            if (models == null || !models.isArray()) return Collections.emptyList();
            List<String> names = new ArrayList<>();
            for (JsonNode node : models) {
                JsonNode name = node.get("name");
                if (name != null && name.isTextual()) {
                    String s = name.asText().trim();
                    if (!s.isEmpty()) names.add(s);
                }
            }
            return names;
        } catch (Exception e) {
            log.warn("Failed to parse Ollama models response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> discoverAnthropic() {
        return new ArrayList<>(ANTHROPIC_MODELS);
    }

    private List<String> discoverBedrock() {
        return new ArrayList<>(BEDROCK_MODELS);
    }
}
