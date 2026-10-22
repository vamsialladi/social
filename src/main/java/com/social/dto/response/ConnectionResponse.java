package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.social.entity.ConnectionStatus;

import java.time.Instant;

public class ConnectionResponse {

    @JsonProperty("_id")
    private String mongoId;

    private String id;
    private String requesterId;
    private String recipientId;
    private ConnectionStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    // Optional enriched user details for frontend convenience
    private UserProfileResponse requester;
    private UserProfileResponse recipient;
    private UserProfileResponse connectedUser;

    public ConnectionResponse() {
    }

    public ConnectionResponse(String mongoId, String id, String requesterId, String recipientId,
                              ConnectionStatus status, Instant createdAt, Instant updatedAt,
                              UserProfileResponse requester, UserProfileResponse recipient,
                              UserProfileResponse connectedUser) {
        this.mongoId = mongoId;
        this.id = id;
        this.requesterId = requesterId;
        this.recipientId = recipientId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.requester = requester;
        this.recipient = recipient;
        this.connectedUser = connectedUser;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getMongoId() {
        return id != null ? id : mongoId;
    }

    public void setMongoId(String mongoId) {
        this.mongoId = mongoId;
    }

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

    public ConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectionStatus status) {
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

    public UserProfileResponse getRequester() {
        return requester;
    }

    public void setRequester(UserProfileResponse requester) {
        this.requester = requester;
    }

    public UserProfileResponse getRecipient() {
        return recipient;
    }

    public void setRecipient(UserProfileResponse recipient) {
        this.recipient = recipient;
    }

    public UserProfileResponse getConnectedUser() {
        return connectedUser;
    }

    public void setConnectedUser(UserProfileResponse connectedUser) {
        this.connectedUser = connectedUser;
    }

    public static class Builder {
        private String mongoId;
        private String id;
        private String requesterId;
        private String recipientId;
        private ConnectionStatus status;
        private Instant createdAt;
        private Instant updatedAt;
        private UserProfileResponse requester;
        private UserProfileResponse recipient;
        private UserProfileResponse connectedUser;

        public Builder mongoId(String mongoId) { this.mongoId = mongoId; return this; }
        public Builder id(String id) { this.id = id; return this; }
        public Builder requesterId(String requesterId) { this.requesterId = requesterId; return this; }
        public Builder recipientId(String recipientId) { this.recipientId = recipientId; return this; }
        public Builder status(ConnectionStatus status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder requester(UserProfileResponse requester) { this.requester = requester; return this; }
        public Builder recipient(UserProfileResponse recipient) { this.recipient = recipient; return this; }
        public Builder connectedUser(UserProfileResponse connectedUser) { this.connectedUser = connectedUser; return this; }

        public ConnectionResponse build() {
            return new ConnectionResponse(mongoId, id, requesterId, recipientId, status, createdAt, updatedAt, requester, recipient, connectedUser);
        }
    }
}
