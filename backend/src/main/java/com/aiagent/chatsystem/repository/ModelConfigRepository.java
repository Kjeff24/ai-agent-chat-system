package com.aiagent.chatsystem.repository;

import com.aiagent.chatsystem.model.ModelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModelConfigRepository extends JpaRepository<ModelConfig, UUID> {
    Optional<ModelConfig> findByIsDefaultTrueAndIsActiveTrue();
    
    List<ModelConfig> findByIsActiveTrueOrderByIsDefaultDescCreatedAtDesc();
    
    @Query("SELECT m FROM ModelConfig m WHERE m.isActive = true ORDER BY m.isDefault DESC, m.createdAt DESC")
    List<ModelConfig> findActiveConfigs();
    
    boolean existsByName(String name);
}
