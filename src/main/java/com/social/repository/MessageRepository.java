package com.social.repository;

import com.social.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    List<Message> findByConversationIdAndCreatedAtLessThanOrderByCreatedAtDesc(
            String conversationId, Instant cursor, Pageable pageable);

    List<Message> findByConversationIdOrderByCreatedAtDesc(
            String conversationId, Pageable pageable);

    long countByConversationIdAndSenderIdNotAndReadAtIsNull(
            String conversationId, String currentUserId);

    Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(String conversationId);

    @Modifying
    @Query("UPDATE Message m SET m.readAt = :readAt " +
           "WHERE m.conversationId = :conversationId AND m.senderId <> :userId AND m.readAt IS NULL")
    int markMessagesAsRead(@Param("conversationId") String conversationId,
                           @Param("userId") String userId,
                           @Param("readAt") Instant readAt);
}
