package com.social.event;

import java.time.Instant;
import java.util.List;

public class MessageSentEvent {
    private String conversationId;
    private String messageId;
    private String senderId;
    private List<String> recipientIds;
    private Instant createdAt;

    public MessageSentEvent() {
    }

    public MessageSentEvent(String conversationId, String messageId, String senderId, List<String> recipientIds, Instant createdAt) {
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.senderId = senderId;
        this.recipientIds = recipientIds;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String conversationId;
        private String messageId;
        private String senderId;
        private List<String> recipientIds;
        private Instant createdAt;

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder senderId(String senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder recipientIds(List<String> recipientIds) {
            this.recipientIds = recipientIds;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public MessageSentEvent build() {
            return new MessageSentEvent(conversationId, messageId, senderId, recipientIds, createdAt);
        }
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public List<String> getRecipientIds() {
        return recipientIds;
    }

    public void setRecipientIds(List<String> recipientIds) {
        this.recipientIds = recipientIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
