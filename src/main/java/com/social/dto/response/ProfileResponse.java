package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.social.entity.Profile;

import java.time.Instant;

public class ProfileResponse {

    @JsonProperty("_id")
    private String _id;

    @JsonProperty("id")
    private String id;

    private String userId;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String headline;
    private String location;
    private Instant createdAt;
    private Instant updatedAt;

    public ProfileResponse() {
    }

    public ProfileResponse(String id, String userId, String displayName, String bio, String avatarUrl,
                           String headline, String location, Instant createdAt, Instant updatedAt) {
        this._id = id;
        this.id = id;
        this.userId = userId;
        this.displayName = displayName;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.headline = headline;
        this.location = location;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProfileResponse fromEntity(Profile profile) {
        if (profile == null) {
            return null;
        }
        return new ProfileResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getDisplayName(),
                profile.getBio(),
                profile.getAvatarUrl(),
                profile.getHeadline(),
                profile.getLocation(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
