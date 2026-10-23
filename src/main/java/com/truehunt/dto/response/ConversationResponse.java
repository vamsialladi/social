package com.truehunt.dto.response;

import java.time.Instant;
import java.util.List;

public class ConversationResponse {
    private String id;
    private List<String> participantIds;
    private List<ProfileResponse> participants;
    private MessageResponse lastMessage;
    private Instant lastMessageAt;
    private Instant createdAt;

    public ConversationResponse() {}

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

    public List<ProfileResponse> getParticipants() {
        return participants;
    }

    public void setParticipants(List<ProfileResponse> participants) {
        this.participants = participants;
    }

    public MessageResponse getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(MessageResponse lastMessage) {
        this.lastMessage = lastMessage;
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
}
