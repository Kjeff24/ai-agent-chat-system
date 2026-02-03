package com.aiagent.chatsystem.repository;

import com.aiagent.chatsystem.model.McpServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface McpServerRepository extends JpaRepository<McpServer, String> {

    List<McpServer> findAllByOrderByCreatedAtAsc();

    Optional<McpServer> findByName(String name);

    boolean existsByName(String name);

    void deleteByName(String name);
}
