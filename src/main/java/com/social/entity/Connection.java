package com.social.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "connections",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_conn_pair", columnNames = {"requester_id", "recipient_id"})
        },
        indexes = {
                @Index(name = "idx_conn_requester", columnList = "requester_id"),
                @Index(name = "idx_conn_recipient", columnList = "recipient_id"),
                @Index(name = "idx_conn_status", columnList = "status"),
                @Index(name = "idx_conn_pair", columnList = "requester_id, recipient_id")
        })
public class Connection {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "requester_id", length = 36, nullable = false)
    private String requesterId;

    @Column(name = "recipient_id", length = 36, nullable = false)
    private String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ConnectionStatus status = ConnectionStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Connection() {
    }

    public Connection(String id, String requesterId, String recipientId, ConnectionStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.requesterId = requesterId;
        this.recipientId = recipientId;
        this.status = status != null ? status : ConnectionStatus.PENDING;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        this.updatedAt = Instant.now();
        if (this.status == null) {
            this.status = ConnectionStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String requesterId;
        private String recipientId;
        private ConnectionStatus status = ConnectionStatus.PENDING;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder requesterId(String requesterId) {
            this.requesterId = requesterId;
            return this;
        }

        public Builder recipientId(String recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        public Builder status(ConnectionStatus status) {
            this.status = status != null ? status : ConnectionStatus.PENDING;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Connection build() {
            return new Connection(id, requesterId, recipientId, status, createdAt, updatedAt);
        }
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

    public ConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectionStatus status) {
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
