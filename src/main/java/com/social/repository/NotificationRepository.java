package com.social.repository;

import com.social.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    List<Notification> findByRecipientIdAndCreatedAtLessThanOrderByCreatedAtDesc(
            String recipientId, Instant cursor, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(String recipientId);

    Optional<Notification> findByIdAndRecipientId(String id, String recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt " +
           "WHERE n.recipientId = :recipientId AND n.id IN :ids AND n.readAt IS NULL")
    int markBatchAsRead(@Param("recipientId") String recipientId,
                        @Param("ids") Collection<String> ids,
                        @Param("readAt") Instant readAt);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt " +
           "WHERE n.recipientId = :recipientId AND n.readAt IS NULL")
    int markAllAsRead(@Param("recipientId") String recipientId,
                      @Param("readAt") Instant readAt);
}
