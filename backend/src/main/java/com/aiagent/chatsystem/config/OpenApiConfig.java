package com.aiagent.chatsystem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Agent Chat System API")
                        .description("REST API for the scalable chat system with configurable AI models. " +
                                "Supports conversations, real-time messaging via WebSocket, and model configuration. " +
                                "Authenticate via /api/auth/login or /api/auth/register, then use the returned JWT in the Authorize button.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AI Agent Chat System")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT from /api/auth/login or /api/auth/register")));
    }
}
