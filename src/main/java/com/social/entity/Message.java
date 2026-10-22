package com.social.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_msg_conv_created", columnList = "conversation_id, created_at"),
        @Index(name = "idx_msg_conversation_id", columnList = "conversation_id"),
        @Index(name = "idx_msg_sender_id", columnList = "sender_id"),
        @Index(name = "idx_msg_created_at", columnList = "created_at")
})
public class Message {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    @JsonProperty("id")
    private String id;

    @Column(name = "conversation_id", length = 36, nullable = false)
    private String conversationId;

    @Column(name = "sender_id", length = 36, nullable = false)
    private String senderId;

    @Column(name = "body", length = 4000)
    private String body;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "message_attachments",
            joinColumns = @JoinColumn(name = "message_id")
    )
    private List<Attachment> attachments = new ArrayList<>();

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Message() {
    }

    public Message(String id, String conversationId, String senderId, String body,
                   List<Attachment> attachments, Instant readAt, Instant createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.body = body;
        this.attachments = attachments != null ? attachments : new ArrayList<>();
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
        private String conversationId;
        private String senderId;
        private String body;
        private List<Attachment> attachments = new ArrayList<>();
        private Instant readAt;
        private Instant createdAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder senderId(String senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder attachments(List<Attachment> attachments) {
            this.attachments = attachments;
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

        public Message build() {
            return new Message(id, conversationId, senderId, body, attachments, readAt, createdAt);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
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
