package com.aiagent.chatsystem.repository;

import com.aiagent.chatsystem.model.SystemPromptOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemPromptOverrideRepository extends JpaRepository<SystemPromptOverride, String> {

    Optional<SystemPromptOverride> findById(String id);
}
