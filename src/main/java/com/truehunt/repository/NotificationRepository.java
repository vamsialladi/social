package com.truehunt.repository;

import com.truehunt.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByRecipientIdAndCreatedAtBeforeOrderByCreatedAtDesc(String recipientId, Instant cursor, Pageable pageable);

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(String recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.recipientId = :recipientId AND n.readAt IS NULL")
    int markAllAsReadForUser(@Param("recipientId") String recipientId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.id IN :ids AND n.recipientId = :recipientId")
    int markAsReadByIds(@Param("ids") List<String> ids, @Param("recipientId") String recipientId, @Param("now") Instant now);
}
