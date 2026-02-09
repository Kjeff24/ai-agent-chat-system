package com.aiagent.chatsystem.repository;

import com.aiagent.chatsystem.model.McpOAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface McpOAuthProviderRepository extends JpaRepository<McpOAuthProvider, String> {

    List<McpOAuthProvider> findAllByOrderByCreatedAtAsc();

    Optional<McpOAuthProvider> findByProviderId(String providerId);

    boolean existsByProviderId(String providerId);

    void deleteByProviderId(String providerId);
}
