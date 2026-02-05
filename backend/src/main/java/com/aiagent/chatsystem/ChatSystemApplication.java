package com.aiagent.chatsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration;

/**
 * Main application class.
 *
 * OpenAI and Anthropic auto-configurations are excluded so the app can start
 * without their API keys. Those providers are only enabled when configured.
 * Ollama remains auto-configured (default base-url).
 * SwaggerConfig is excluded by name because it references Spring Boot 3's WebMvcProperties,
 * which was moved in Spring Boot 4; excluding by class would load it and fail.
 */
@SpringBootApplication(
    exclude = {
        OpenAiChatAutoConfiguration.class,
        AnthropicChatAutoConfiguration.class
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
