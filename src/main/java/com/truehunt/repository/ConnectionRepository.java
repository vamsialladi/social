package com.truehunt.repository;

import com.truehunt.model.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, String> {

    @Query("SELECT c FROM Connection c WHERE (c.requesterId = :u1 AND c.recipientId = :u2) OR (c.requesterId = :u2 AND c.recipientId = :u1)")
    Optional<Connection> findConnectionBetween(@Param("u1") String u1, @Param("u2") String u2);

    @Query("SELECT c FROM Connection c WHERE c.status = 'accepted' AND (c.requesterId = :userId OR c.recipientId = :userId)")
    List<Connection> findAcceptedConnections(@Param("userId") String userId);

    @Query("SELECT c FROM Connection c WHERE (c.requesterId = :userId OR c.recipientId = :userId)")
    List<Connection> findAllForUser(@Param("userId") String userId);
}
