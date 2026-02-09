package com.aiagent.chatsystem.repository;

import com.aiagent.chatsystem.model.McpOAuthToken;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface McpOAuthTokenRepository extends JpaRepository<McpOAuthToken, UUID> {

    Optional<McpOAuthToken> findByUserIdAndServerName(UUID userId, String serverName);

    void deleteByUserIdAndServerName(UUID userId, String serverName);

    boolean existsByUserIdAndServerName(UUID userId, String serverName);
}
