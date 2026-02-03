package com.aiagent.chatsystem.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Per-user OAuth token for an MCP server that uses OAuth (e.g. Atlassian).
 * One row per (userId, serverName).
 */
@Entity
@Table(name = "mcp_oauth_tokens", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "server_name"})
})
public class McpOAuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "server_name", nullable = false, length = 255)
    private String serverName;

    @Column(name = "access_token", nullable = false, length = 4096)
    private String accessToken;

    @Column(name = "refresh_token", length = 4096)
    private String refreshToken;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** True if the token is expired or will expire within 60 seconds. */
    public boolean isExpired() {
        return expiresAt == null || expiresAt.isBefore(Instant.now().plusSeconds(60));
    }
}
