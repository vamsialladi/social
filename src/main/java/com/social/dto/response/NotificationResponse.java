package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.social.entity.Notification;
import com.social.entity.NotificationType;

import java.time.Instant;

public class NotificationResponse {

    @JsonProperty("id")
    private String id;

    private String recipientId;
    private String actorId;
    private NotificationType type;
    private String resourceType;
    private String resourceId;
    private Instant readAt;
    private Instant createdAt;

    public NotificationResponse() {
    }

    public NotificationResponse(String id, String recipientId, String actorId, NotificationType type,
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

    @JsonProperty("_id")
    public String get_id() {
        return id;
    }

    public static NotificationResponse fromEntity(Notification notification) {
        if (notification == null) return null;
        return NotificationResponse.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipientId())
                .actorId(notification.getActorId())
                .type(notification.getType())
                .resourceType(notification.getResourceType())
                .resourceId(notification.getResourceId())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
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

        public NotificationResponse build() {
            return new NotificationResponse(id, recipientId, actorId, type, resourceType, resourceId, readAt, createdAt);
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
