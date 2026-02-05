package com.aiagent.chatsystem.service;

import com.aiagent.chatsystem.dto.RegisterModelRequest;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

/**
 * Builds a ChatModel from a RegisterModelRequest (OpenAI-compatible, Anthropic, or Ollama).
 */
@Component
public class ModelFactory {

    private static final String OPENAI_DEFAULT_BASE = "https://api.openai.com";
    private static final String OPENAI_DEFAULT_MODEL = "gpt-4";
    private static final String ANTHROPIC_DEFAULT_BASE = "https://api.anthropic.com";
    private static final String ANTHROPIC_DEFAULT_MODEL = "claude-3-5-sonnet-latest";
    private static final String OLLAMA_DEFAULT_BASE = "http://localhost:11434";
    private static final String OLLAMA_DEFAULT_MODEL = "llama2";

    /**
     * Build a ChatModel from the request. Supports type "openai" (OpenAI API, OpenRouter, etc.),
     * "anthropic", and "ollama". Default model is resolved from defaultModel, or first of models list, or type default.
     */
    public ChatModel build(RegisterModelRequest request) {
        if (request == null || request.getType() == null) {
            throw new IllegalArgumentException("Request and type are required");
        }
        String type = request.getType().trim().toLowerCase();
        return switch (type) {
            case "openai" -> buildOpenAi(request);
            case "anthropic" -> buildAnthropic(request);
            case "ollama" -> buildOllama(request);
            default -> throw new IllegalArgumentException(
                    "Unsupported type: " + type + ". Use 'openai', 'anthropic', or 'ollama'.");
        };
    }

    /**
     * Resolve the default model for the request: explicit defaultModel, or first of models list, or type default.
     * Validates that if both models and defaultModel are set, defaultModel is in models.
     */
    public String resolveDefaultModel(RegisterModelRequest request, String typeDefault) {
        if (request == null) return typeDefault;
        String explicit = request.getDefaultModel() != null && !request.getDefaultModel().isBlank()
                ? request.getDefaultModel().trim()
                : null;
        var modelsList = request.getModels();
        boolean hasList = modelsList != null && !modelsList.isEmpty();
        if (explicit != null) {
            if (hasList && !modelsList.stream().anyMatch(m -> m != null && m.trim().equalsIgnoreCase(explicit))) {
                throw new IllegalArgumentException(
                        "defaultModel must be one of models when models is provided. defaultModel='" + explicit + "', models=" + modelsList);
            }
            return explicit;
        }
        if (hasList) {
            String first = modelsList.stream().filter(m -> m != null && !m.isBlank()).findFirst().map(String::trim).orElse(null);
            if (first != null) return first;
        }
        return typeDefault;
    }

    private ChatModel buildOpenAi(RegisterModelRequest request) {
        String apiKey = request.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key is required for type 'openai'");
        }
        String baseUrl = request.getBaseUrl() != null && !request.getBaseUrl().isBlank()
                ? request.getBaseUrl().trim()
                : OPENAI_DEFAULT_BASE;
        String model = resolveDefaultModel(request, OPENAI_DEFAULT_MODEL);

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
    }

    private ChatModel buildAnthropic(RegisterModelRequest request) {
        String apiKey = request.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key is required for type 'anthropic'");
        }
        String baseUrl = request.getBaseUrl() != null && !request.getBaseUrl().isBlank()
                ? request.getBaseUrl().trim()
                : ANTHROPIC_DEFAULT_BASE;
        String model = resolveDefaultModel(request, ANTHROPIC_DEFAULT_MODEL);

        AnthropicApi api = AnthropicApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(model)
                .build();
        return AnthropicChatModel.builder()
                .anthropicApi(api)
                .defaultOptions(options)
                .build();
    }

    private ChatModel buildOllama(RegisterModelRequest request) {
        String baseUrl = request.getBaseUrl() != null && !request.getBaseUrl().isBlank()
                ? request.getBaseUrl().trim()
                : OLLAMA_DEFAULT_BASE;
        String model = resolveDefaultModel(request, OLLAMA_DEFAULT_MODEL);

        OllamaApi api = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model(model)
                .build();
        return OllamaChatModel.builder()
                .ollamaApi(api)
                .defaultOptions(options)
                .build();
    }
}
