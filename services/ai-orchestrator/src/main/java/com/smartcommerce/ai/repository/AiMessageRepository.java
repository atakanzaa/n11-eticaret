package com.smartcommerce.ai.repository;

import com.smartcommerce.ai.domain.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {
    List<AiMessage> findTop20ByConversationIdOrderByCreatedAtAsc(UUID conversationId);
    List<AiMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
