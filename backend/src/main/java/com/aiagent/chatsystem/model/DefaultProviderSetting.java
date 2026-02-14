package com.aiagent.chatsystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Persisted default model provider key (set via PUT /api/models/registry/default-provider).
 * Single row; when absent or empty, the first registered provider is used.
 */
@Entity
@Table(name = "default_provider_setting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefaultProviderSetting {

    public static final String DEFAULT_ID = "default";

    @Id
    @Column(nullable = false, unique = true, length = 64)
    private String id = DEFAULT_ID;

    @Column(name = "provider_key", length = 128)
    private String providerKey;

    public static DefaultProviderSetting create(String providerKey) {
        DefaultProviderSetting s = new DefaultProviderSetting();
        s.setId(DEFAULT_ID);
        s.setProviderKey(providerKey);
        return s;
    }
}
