package com.aiagent.chatsystem.repository;

import com.aiagent.chatsystem.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(UUID userId);
    
    @Query("SELECT c FROM Conversation c WHERE c.userId = :userId ORDER BY c.updatedAt DESC")
    List<Conversation> findUserConversations(@Param("userId") UUID userId);
    
    boolean existsByIdAndUserId(UUID id, UUID userId);
}
