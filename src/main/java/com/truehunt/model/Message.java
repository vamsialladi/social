package com.truehunt.model;

import com.truehunt.model.converter.MediaAttachmentListConverter;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_msg_conversation", columnList = "conversation_id, created_at"),
    @Index(name = "idx_msg_sender", columnList = "sender_id")
})
public class Message {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "conversation_id", length = 36, nullable = false)
    private String conversationId;

    @Column(name = "sender_id", length = 36, nullable = false)
    private String senderId;

    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    @Convert(converter = MediaAttachmentListConverter.class)
    @Column(name = "attachments", columnDefinition = "TEXT")
    private List<MediaAttachment> attachments = new ArrayList<>();

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Message() {}

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        this.createdAt = now;
        if (this.attachments == null) this.attachments = new ArrayList<>();
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

    public List<MediaAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MediaAttachment> attachments) {
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
