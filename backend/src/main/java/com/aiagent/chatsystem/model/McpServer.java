package com.aiagent.chatsystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persisted MCP server registration (dynamically added via API).
 * Loaded on startup so servers survive restarts. Static servers remain in application.yml.
 */
@Entity
@Table(name = "mcp_servers", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpServer {

    @Id
    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(name = "request_timeout_seconds", nullable = false)
    private int requestTimeoutSeconds = 30;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> headers = new LinkedHashMap<>();

    @Column(name = "oauth_provider", length = 64)
    private String oauthProvider;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
