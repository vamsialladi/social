package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class UserProfileResponse {

    @JsonProperty("_id")
    private String mongoId;

    private String id;
    private String username;
    private String name;
    private String email;
    private String headline;
    private String avatarUrl;
    private String bio;
    private String role;
    private String location;
    private Instant createdAt;

    public UserProfileResponse() {
    }

    public UserProfileResponse(String mongoId, String id, String username, String name, String email,
                               String headline, String avatarUrl, String bio, String role, String location,
                               Instant createdAt) {
        this.mongoId = mongoId;
        this.id = id;
        this.username = username;
        this.name = name;
        this.email = email;
        this.headline = headline;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.role = role;
        this.location = location;
        this.createdAt = createdAt;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static class Builder {
        private String mongoId;
        private String id;
        private String username;
        private String name;
        private String email;
        private String headline;
        private String avatarUrl;
        private String bio;
        private String role;
        private String location;
        private Instant createdAt;

        public Builder mongoId(String mongoId) { this.mongoId = mongoId; return this; }
        public Builder id(String id) { this.id = id; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder headline(String headline) { this.headline = headline; return this; }
        public Builder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
        public Builder bio(String bio) { this.bio = bio; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder location(String location) { this.location = location; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public UserProfileResponse build() {
            return new UserProfileResponse(mongoId, id, username, name, email, headline, avatarUrl, bio, role, location, createdAt);
        }
    }
}
