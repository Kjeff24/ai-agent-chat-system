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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.FoundationModelSummary;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsRequest;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsResponse;
import software.amazon.awssdk.services.bedrock.model.ModelModality;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Discovers model IDs from OpenAI (GET /v1/models), Ollama (GET /api/tags),
 * and returns curated lists for Anthropic and Bedrock (no list API).
 * Bedrock: uses ListFoundationModels (control plane) when credentials/region are provided;
 * falls back to a curated list on failure. Requires IAM bedrock:ListFoundationModels for dynamic discovery.
 * See https://docs.aws.amazon.com/bedrock/latest/userguide/model-ids.html and
 * https://docs.spring.io/spring-ai/reference/api/chat/bedrock-converse.html
 */
@Service
public class ModelDiscoveryServiceImpl implements ModelDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(ModelDiscoveryServiceImpl.class);

    private static final String OPENAI_DEFAULT_BASE = "https://api.openai.com";
    private static final String OLLAMA_DEFAULT_BASE = "http://localhost:11434";
    private static final String BEDROCK_DEFAULT_REGION = "us-east-1";

    private static final List<String> ANTHROPIC_MODELS = List.of(
            "claude-3-5-sonnet-20241022",
            "claude-3-5-sonnet-latest",
            "claude-3-opus-20240229",
            "claude-3-sonnet-20240229",
            "claude-3-haiku-20240307"
    );

    /** Converse-capable Bedrock chat models (region availability may vary). */
    private static final List<String> BEDROCK_MODELS = List.of(
            // Anthropic Claude
            "anthropic.claude-3-5-sonnet-20240620-v1:0",
            "anthropic.claude-3-5-haiku-20241022-v1:0",
            "anthropic.claude-3-sonnet-20240229-v1:0",
            "anthropic.claude-3-haiku-20240307-v1:0",
            "anthropic.claude-haiku-4-5-20251001-v1:0",
            "anthropic.claude-opus-4-1-20250805-v1:0",
            "anthropic.claude-opus-4-5-20251101-v1:0",
            "anthropic.claude-opus-4-6-v1",
            "anthropic.claude-sonnet-4-5-20250929-v1:0",
            "anthropic.claude-sonnet-4-20250514-v1:0",
            // Amazon Nova (chat)
            "amazon.nova-micro-v1:0",
            "amazon.nova-lite-v1:0",
            "amazon.nova-pro-v1:0",
            "amazon.nova-premier-v1:0",
            "amazon.nova-2-lite-v1:0",
            "amazon.titan-tg1-large",
            // Cohere
            "cohere.command-r-v1:0",
            "cohere.command-r-plus-v1:0",
            // Meta Llama
            "meta.llama3-8b-instruct-v1:0",
            "meta.llama3-70b-instruct-v1:0",
            "meta.llama3-1-8b-instruct-v1:0",
            "meta.llama3-1-70b-instruct-v1:0",
            "meta.llama3-1-405b-instruct-v1:0",
            "meta.llama3-2-1b-instruct-v1:0",
            "meta.llama3-2-3b-instruct-v1:0",
            "meta.llama3-2-11b-instruct-v1:0",
            "meta.llama3-2-90b-instruct-v1:0",
            "meta.llama3-3-70b-instruct-v1:0",
            "meta.llama4-maverick-17b-instruct-v1:0",
            "meta.llama4-scout-17b-instruct-v1:0",
            // Mistral
            "mistral.mistral-7b-instruct-v0:2",
            "mistral.mistral-small-2402-v1:0",
            "mistral.mistral-large-2402-v1:0",
            "mistral.mistral-large-2407-v1:0",
            "mistral.mistral-large-3-675b-instruct",
            "mistral.mixtral-8x7b-instruct-v0:1",
            "mistral.pixtral-large-2502-v1:0",
            "mistral.magistral-small-2509",
            "mistral.ministral-3-3b-instruct",
            "mistral.ministral-3-8b-instruct",
            "mistral.ministral-3-14b-instruct",
            // AI21, DeepSeek, Google
            "ai21.jamba-1-5-mini-v1:0",
            "ai21.jamba-1-5-large-v1:0",
            "deepseek.r1-v1:0",
            "deepseek.v3-v1:0",
            "google.gemma-3-4b-it",
            "google.gemma-3-12b-it",
            "google.gemma-3-27b-it"
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
            case "bedrock" -> discoverBedrock(request);
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

    private List<String> discoverBedrock(DiscoverModelsRequest request) {
        String regionStr = request.getBaseUrl() != null && !request.getBaseUrl().isBlank()
                ? request.getBaseUrl().trim()
                : BEDROCK_DEFAULT_REGION;
        AwsCredentialsProvider credentialsProvider = resolveBedrockCredentials(request);

        try (BedrockClient bedrockClient = BedrockClient.builder()
                .region(Region.of(regionStr))
                .credentialsProvider(credentialsProvider)
                .build()) {
            ListFoundationModelsResponse response = bedrockClient.listFoundationModels(
                    ListFoundationModelsRequest.builder()
                            .byOutputModality(ModelModality.TEXT)
                            .build());
            List<String> modelIds = new ArrayList<>();
            if (response.modelSummaries() != null) {
                for (FoundationModelSummary summary : response.modelSummaries()) {
                    if (summary.modelId() != null && !summary.modelId().isBlank()) {
                        modelIds.add(summary.modelId().trim());
                    }
                }
            }
            if (!modelIds.isEmpty()) {
                log.debug("Bedrock dynamic discovery returned {} models for region {}", modelIds.size(), regionStr);
                return modelIds;
            }
        } catch (Exception e) {
            log.warn("Bedrock ListFoundationModels failed ({}), falling back to curated list: {}",
                    regionStr, e.getMessage());
        }
        return new ArrayList<>(BEDROCK_MODELS);
    }

    private static AwsCredentialsProvider resolveBedrockCredentials(DiscoverModelsRequest request) {
        String accessKey = request.getApiKey() != null ? request.getApiKey().trim() : "";
        String secretKey = request.getSecretKey() != null ? request.getSecretKey().trim() : "";
        if (!accessKey.isEmpty() && !secretKey.isEmpty()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }
        return DefaultCredentialsProvider.builder().build();
    }
}
