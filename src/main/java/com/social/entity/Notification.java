package com.social.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_recipient_created", columnList = "recipient_id, created_at DESC"),
        @Index(name = "idx_notif_recipient", columnList = "recipient_id")
})
public class Notification {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "recipient_id", length = 36, nullable = false)
    private String recipientId;

    @Column(name = "sender_id", length = 36)
    private String senderId;

    @Column(name = "type", length = 50, nullable = false)
    private String type; // e.g. "CONNECTION_REQUEST", "CONNECTION_ACCEPTED"

    @Column(name = "message", length = 500, nullable = false)
    private String message;

    @Column(name = "reference_id", length = 36)
    private String referenceId; // e.g. connectionId

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Notification() {
    }

    public Notification(String id, String recipientId, String senderId, String type, String message, String referenceId, boolean read, Instant createdAt) {
        this.id = id;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.type = type;
        this.message = message;
        this.referenceId = referenceId;
        this.read = read;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String recipientId;
        private String senderId;
        private String type;
        private String message;
        private String referenceId;
        private boolean read = false;
        private Instant createdAt = Instant.now();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder recipientId(String recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        public Builder senderId(String senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder referenceId(String referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        public Builder read(boolean read) {
            this.read = read;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Notification build() {
            return new Notification(id, recipientId, senderId, type, message, referenceId, read, createdAt);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
