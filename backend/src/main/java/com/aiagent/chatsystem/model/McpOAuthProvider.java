package com.aiagent.chatsystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Persisted OAuth provider config (dynamically added via API).
 * Loaded on startup so providers survive restarts. Static providers remain in application.yml.
 * Client secret is stored as-is; consider encryption in production.
 */
@Entity
@Table(name = "mcp_oauth_providers", uniqueConstraints = @UniqueConstraint(columnNames = "provider_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpOAuthProvider {

    @Id
    @Column(name = "provider_id", nullable = false, unique = true, length = 64)
    private String providerId;

    @Column(name = "authorization_uri", nullable = false, length = 2048)
    private String authorizationUri;

    @Column(name = "token_uri", nullable = false, length = 2048)
    private String tokenUri;

    @Column(name = "client_id", length = 512)
    private String clientId;

    @Column(name = "client_secret", length = 1024)
    private String clientSecret;

    @Column(length = 1024)
    private String scopes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
