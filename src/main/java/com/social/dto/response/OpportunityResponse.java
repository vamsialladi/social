package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public class OpportunityResponse {

    @JsonProperty("_id")
    private String mongoId;

    private String id;
    private String authorId;
    private String authorName;
    private String authorAvatarUrl;
    private String title;
    private String content;
    private String type;
    private List<String> mediaUrls;
    private String employer;
    private String compensation;
    private String location;
    private Instant expiryDate;
    private Integer applicationsCount;
    private Instant createdAt;
    private Instant updatedAt;

    public OpportunityResponse() {
    }

    public OpportunityResponse(String mongoId, String id, String authorId, String authorName, String authorAvatarUrl,
                               String title, String content, String type, List<String> mediaUrls, String employer,
                               String compensation, String location, Instant expiryDate, Integer applicationsCount,
                               Instant createdAt, Instant updatedAt) {
        this.mongoId = mongoId;
        this.id = id;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.title = title;
        this.content = content;
        this.type = type;
        this.mediaUrls = mediaUrls;
        this.employer = employer;
        this.compensation = compensation;
        this.location = location;
        this.expiryDate = expiryDate;
        this.applicationsCount = applicationsCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    public String getEmployer() {
        return employer;
    }

    public void setEmployer(String employer) {
        this.employer = employer;
    }

    public String getCompensation() {
        return compensation;
    }

    public void setCompensation(String compensation) {
        this.compensation = compensation;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Integer getApplicationsCount() {
        return applicationsCount;
    }

    public void setApplicationsCount(Integer applicationsCount) {
        this.applicationsCount = applicationsCount;
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

    public static class Builder {
        private String mongoId;
        private String id;
        private String authorId;
        private String authorName;
        private String authorAvatarUrl;
        private String title;
        private String content;
        private String type;
        private List<String> mediaUrls;
        private String employer;
        private String compensation;
        private String location;
        private Instant expiryDate;
        private Integer applicationsCount;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder mongoId(String mongoId) { this.mongoId = mongoId; return this; }
        public Builder id(String id) { this.id = id; return this; }
        public Builder authorId(String authorId) { this.authorId = authorId; return this; }
        public Builder authorName(String authorName) { this.authorName = authorName; return this; }
        public Builder authorAvatarUrl(String authorAvatarUrl) { this.authorAvatarUrl = authorAvatarUrl; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder mediaUrls(List<String> mediaUrls) { this.mediaUrls = mediaUrls; return this; }
        public Builder employer(String employer) { this.employer = employer; return this; }
        public Builder compensation(String compensation) { this.compensation = compensation; return this; }
        public Builder location(String location) { this.location = location; return this; }
        public Builder expiryDate(Instant expiryDate) { this.expiryDate = expiryDate; return this; }
        public Builder applicationsCount(Integer applicationsCount) { this.applicationsCount = applicationsCount; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public OpportunityResponse build() {
            return new OpportunityResponse(mongoId, id, authorId, authorName, authorAvatarUrl, title, content, type,
                    mediaUrls, employer, compensation, location, expiryDate, applicationsCount, createdAt, updatedAt);
        }
    }
}
