package com.aiagent.chatsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;
import org.springframework.ai.autoconfigure.anthropic.AnthropicAutoConfiguration;

/**
 * Main application class.
 *
 * OpenAI and Anthropic auto-configurations are excluded so the app can start
 * without their API keys. Those providers are only enabled when configured.
 * Ollama remains auto-configured (default base-url).
 */
@SpringBootApplication(exclude = {
    OpenAiAutoConfiguration.class,
    AnthropicAutoConfiguration.class
})
public class ChatSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatSystemApplication.class, args);
    }
}
