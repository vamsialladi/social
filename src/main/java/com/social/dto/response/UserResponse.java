package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.social.entity.User;

import java.time.Instant;

public class UserResponse {

    @JsonProperty("_id")
    private String _id;

    @JsonProperty("id")
    private String id;

    private String email;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public UserResponse() {
    }

    public UserResponse(String id, String email, String status, Instant createdAt, Instant updatedAt) {
        this._id = id;
        this.id = id;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getStatus() != null ? user.getStatus().getValue() : "active",
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
        this.id = _id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this._id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
