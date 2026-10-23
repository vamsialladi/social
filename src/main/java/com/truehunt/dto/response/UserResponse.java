package com.truehunt.dto.response;

import java.time.Instant;

public class UserResponse {
    private String id;
    private String email;
    private String status;
    private Instant createdAt;

    public UserResponse() {}

    public UserResponse(String id, String email, String status, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
}
