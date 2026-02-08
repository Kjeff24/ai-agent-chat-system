package com.aiagent.chatsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;

/**
 * Main application class.
 *
 * OpenAI, Anthropic and Ollama auto-configurations are excluded so the app can start without any of them.
 * Add providers at runtime via Settings → Model providers when needed.
 * SwaggerConfig is excluded by name because it references Spring Boot 3's WebMvcProperties,
 * which was moved in Spring Boot 4; excluding by class would load it and fail.
 */
@SpringBootApplication(
    exclude = {
        OpenAiChatAutoConfiguration.class,
        AnthropicChatAutoConfiguration.class,
        OllamaChatAutoConfiguration.class,
        OllamaApiAutoConfiguration.class,
        OllamaEmbeddingAutoConfiguration.class
    },
    excludeName = {
        "org.springdoc.webmvc.ui.SwaggerConfig",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration"
    }
)
public class ChatSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatSystemApplication.class, args);
    }
}
