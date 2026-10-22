package com.social.repository;

import com.social.entity.Connection;
import com.social.entity.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, String> {

    @Query("SELECT c FROM Connection c WHERE (c.requesterId = :user1 AND c.recipientId = :user2) " +
            "OR (c.requesterId = :user2 AND c.recipientId = :user1)")
    Optional<Connection> findConnectionBetween(@Param("user1") String user1, @Param("user2") String user2);

    @Query("SELECT COUNT(c) > 0 FROM Connection c WHERE ((c.requesterId = :user1 AND c.recipientId = :user2) " +
            "OR (c.requesterId = :user2 AND c.recipientId = :user1)) AND c.status IN :statuses")
    boolean existsActiveConnectionBetween(
            @Param("user1") String user1,
            @Param("user2") String user2,
            @Param("statuses") Collection<ConnectionStatus> statuses
    );

    @Query("SELECT c FROM Connection c WHERE (c.requesterId = :userId OR c.recipientId = :userId) " +
            "AND c.status = 'ACCEPTED' ORDER BY c.updatedAt DESC")
    List<Connection> findAcceptedConnectionsForUser(@Param("userId") String userId);

    @Query("SELECT c.recipientId FROM Connection c WHERE c.requesterId = :userId " +
            "UNION " +
            "SELECT c.requesterId FROM Connection c WHERE c.recipientId = :userId")
    List<String> findAllConnectedUserIds(@Param("userId") String userId);

    @Query("SELECT c FROM Connection c WHERE (c.requesterId = :userId OR c.recipientId = :userId) " +
            "AND c.status = 'BLOCKED'")
    List<Connection> findBlockedConnectionsForUser(@Param("userId") String userId);

    Optional<Connection> findByIdAndRecipientId(String id, String recipientId);
}
