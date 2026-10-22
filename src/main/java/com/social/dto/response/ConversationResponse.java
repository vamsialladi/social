package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.social.entity.Conversation;

import java.time.Instant;
import java.util.List;

public class ConversationResponse {

    @JsonProperty("id")
    private String id;

    private List<String> participantIds;
    private Instant lastMessageAt;
    private Instant createdAt;
    private MessageResponse lastMessage;
    private long unreadCount;

    public ConversationResponse() {
    }

    public ConversationResponse(String id, List<String> participantIds, Instant lastMessageAt,
                                Instant createdAt, MessageResponse lastMessage, long unreadCount) {
        this.id = id;
        this.participantIds = participantIds;
        this.lastMessageAt = lastMessageAt;
        this.createdAt = createdAt;
        this.lastMessage = lastMessage;
        this.unreadCount = unreadCount;
    }

    @JsonProperty("_id")
    public String get_id() {
        return id;
    }

    public static ConversationResponse fromEntity(Conversation conversation) {
        if (conversation == null) return null;
        return ConversationResponse.builder()
                .id(conversation.getId())
                .participantIds(conversation.getParticipantIds())
                .lastMessageAt(conversation.getLastMessageAt())
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private List<String> participantIds;
        private Instant lastMessageAt;
        private Instant createdAt;
        private MessageResponse lastMessage;
        private long unreadCount;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder participantIds(List<String> participantIds) {
            this.participantIds = participantIds;
            return this;
        }

        public Builder lastMessageAt(Instant lastMessageAt) {
            this.lastMessageAt = lastMessageAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastMessage(MessageResponse lastMessage) {
            this.lastMessage = lastMessage;
            return this;
        }

        public Builder unreadCount(long unreadCount) {
            this.unreadCount = unreadCount;
            return this;
        }

        public ConversationResponse build() {
            return new ConversationResponse(id, participantIds, lastMessageAt, createdAt, lastMessage, unreadCount);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public MessageResponse getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(MessageResponse lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
