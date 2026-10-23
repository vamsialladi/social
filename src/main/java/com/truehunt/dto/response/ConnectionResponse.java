package com.truehunt.dto.response;

import java.time.Instant;

public class ConnectionResponse {
    private String id;
    private String requesterId;
    private String recipientId;
    private ProfileResponse requesterProfile;
    private ProfileResponse recipientProfile;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public ConnectionResponse() {}

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

    public ProfileResponse getRequesterProfile() {
        return requesterProfile;
    }

    public void setRequesterProfile(ProfileResponse requesterProfile) {
        this.requesterProfile = requesterProfile;
    }

    public ProfileResponse getRecipientProfile() {
        return recipientProfile;
    }

    public void setRecipientProfile(ProfileResponse recipientProfile) {
        this.recipientProfile = recipientProfile;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
