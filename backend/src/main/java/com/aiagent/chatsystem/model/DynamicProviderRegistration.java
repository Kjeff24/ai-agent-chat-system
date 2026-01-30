package com.aiagent.chatsystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Persisted registration of a dynamically added AI model provider.
 * Loaded on startup so providers survive restarts.
 * API keys are stored as-is; for production consider encryption or a secrets manager.
 */
@Entity
@Table(name = "dynamic_provider_registrations", uniqueConstraints = @UniqueConstraint(columnNames = "provider_key"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DynamicProviderRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @Column(name = "provider_key", nullable = false, unique = true)
    private String providerKey;

    @Column(nullable = false)
    private String type;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "base_url")
    private String baseUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> models;

    @Column(name = "default_model")
    private String defaultModel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
