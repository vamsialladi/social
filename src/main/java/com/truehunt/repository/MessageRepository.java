package com.truehunt.repository;

import com.truehunt.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    List<Message> findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(String conversationId, Instant cursor, Pageable pageable);
    List<Message> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);
    List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);
}
