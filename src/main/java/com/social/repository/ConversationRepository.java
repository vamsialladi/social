package com.social.repository;

import com.social.entity.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    @Query("SELECT DISTINCT c FROM Conversation c JOIN c.participantIds p " +
           "WHERE p = :userId " +
           "ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC")
    List<Conversation> findConversationsByParticipantId(@Param("userId") String userId);

    @Query("SELECT DISTINCT c FROM Conversation c JOIN c.participantIds p " +
           "WHERE p = :userId " +
           "ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC")
    List<Conversation> findConversationsByParticipantId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT DISTINCT c FROM Conversation c JOIN c.participantIds p " +
           "WHERE p = :userId AND c.lastMessageAt < :cursor " +
           "ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC")
    List<Conversation> findConversationsByParticipantIdBefore(
            @Param("userId") String userId,
            @Param("cursor") Instant cursor,
            Pageable pageable);

    @Query("SELECT c FROM Conversation c JOIN c.participantIds p1 JOIN c.participantIds p2 " +
           "WHERE p1 = :user1 AND p2 = :user2 AND SIZE(c.participantIds) = 2")
    Optional<Conversation> findDirectConversation(@Param("user1") String user1, @Param("user2") String user2);
}
