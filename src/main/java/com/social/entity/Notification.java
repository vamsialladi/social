package com.social.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_recipient_created", columnList = "recipient_id, created_at"),
        @Index(name = "idx_notif_recipient_read", columnList = "recipient_id, read_at"),
        @Index(name = "idx_notif_actor", columnList = "actor_id")
})
public class Notification {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    @JsonProperty("id")
    private String id;

    @Column(name = "recipient_id", length = 36, nullable = false)
    private String recipientId;

    @Column(name = "actor_id", length = 36, nullable = false)
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50, nullable = false)
    private NotificationType type;

    @Column(name = "resource_type", length = 50, nullable = false)
    private String resourceType;

    @Column(name = "resource_id", length = 36, nullable = false)
    private String resourceId;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Notification() {
    }

    public Notification(String id, String recipientId, String actorId, NotificationType type,
                        String resourceType, String resourceId, Instant readAt, Instant createdAt) {
        this.id = id;
        this.recipientId = recipientId;
        this.actorId = actorId;
        this.type = type;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @JsonProperty("_id")
    public String get_id() {
        return id;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String recipientId;
        private String actorId;
        private NotificationType type;
        private String resourceType;
        private String resourceId;
        private Instant readAt;
        private Instant createdAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder recipientId(String recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        public Builder actorId(String actorId) {
            this.actorId = actorId;
            return this;
        }

        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public Builder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder readAt(Instant readAt) {
            this.readAt = readAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Notification build() {
            return new Notification(id, recipientId, actorId, type, resourceType, resourceId, readAt, createdAt);
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

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
