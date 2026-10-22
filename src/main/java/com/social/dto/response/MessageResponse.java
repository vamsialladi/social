package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.social.entity.Attachment;
import com.social.entity.Message;

import java.time.Instant;
import java.util.List;

public class MessageResponse {

    @JsonProperty("id")
    private String id;

    private String conversationId;
    private String senderId;
    private String body;
    private List<Attachment> attachments;
    private Instant readAt;
    private Instant createdAt;

    public MessageResponse() {
    }

    public MessageResponse(String id, String conversationId, String senderId, String body,
                           List<Attachment> attachments, Instant readAt, Instant createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.body = body;
        this.attachments = attachments;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    @JsonProperty("_id")
    public String get_id() {
        return id;
    }

    public static MessageResponse fromEntity(Message message) {
        if (message == null) return null;
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .body(message.getBody())
                .attachments(message.getAttachments())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String conversationId;
        private String senderId;
        private String body;
        private List<Attachment> attachments;
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

        public MessageResponse build() {
            return new MessageResponse(id, conversationId, senderId, body, attachments, readAt, createdAt);
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
