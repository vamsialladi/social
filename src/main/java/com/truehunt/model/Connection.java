package com.truehunt.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "connections", indexes = {
    @Index(name = "idx_connection_pair", columnList = "requester_id, recipient_id"),
    @Index(name = "idx_connection_status", columnList = "status"),
    @Index(name = "idx_connection_recipient", columnList = "recipient_id")
})
public class Connection {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "requester_id", length = 36, nullable = false)
    private String requesterId;

    @Column(name = "recipient_id", length = 36, nullable = false)
    private String recipientId;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "pending"; // pending, accepted, rejected, blocked

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Connection() {}

    public Connection(String requesterId, String recipientId, String status) {
        this.requesterId = requesterId;
        this.recipientId = recipientId;
        this.status = status;
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = "pending";
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
