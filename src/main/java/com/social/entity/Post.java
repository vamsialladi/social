package com.social.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_post_type_created", columnList = "type, created_at DESC"),
        @Index(name = "idx_post_author_id", columnList = "author_id"),
        @Index(name = "idx_post_visibility", columnList = "visibility")
})
public class Post {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "author_id", length = 64, nullable = false)
    private String authorId;

    @Column(name = "author_name", length = 150)
    private String authorName;

    @Column(name = "author_avatar_url", length = 500)
    private String authorAvatarUrl;

    @Column(name = "type", length = 50, nullable = false)
    private String type = "post";

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_attachments", joinColumns = @JoinColumn(name = "post_id"))
    private List<MediaFileReference> attachments = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_media_urls", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "media_url", length = 500)
    private List<String> mediaUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private PostVisibility visibility = PostVisibility.PUBLIC;

    @Column(name = "report_count", nullable = false)
    private int reportCount = 0;

    @Column(name = "is_spam", nullable = false)
    private boolean isSpam = false;

    // Structured Opportunity fields
    @Column(name = "employer", length = 150)
    private String employer;

    @Column(name = "compensation", length = 100)
    private String compensation;

    @Column(name = "location", length = 150)
    private String location;

    @Column(name = "expiry_date")
    private Instant expiryDate;

    @Column(name = "applications_count", nullable = false)
    private Integer applicationsCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.type == null) {
            this.type = "post";
        }
        if (this.visibility == null) {
            this.visibility = PostVisibility.PUBLIC;
        }
        if (this.applicationsCount == null) {
            this.applicationsCount = 0;
        }
        if (this.mediaUrls == null) {
            this.mediaUrls = new ArrayList<>();
        }
        if (this.attachments == null) {
            this.attachments = new ArrayList<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String authorId;
        private String authorName;
        private String authorAvatarUrl;
        private String type = "post";
        private String title;
        private String content;
        private List<MediaFileReference> attachments = new ArrayList<>();
        private List<String> mediaUrls = new ArrayList<>();
        private PostVisibility visibility = PostVisibility.PUBLIC;
        private int reportCount = 0;
        private boolean isSpam = false;
        private String employer;
        private String compensation;
        private String location;
        private Instant expiryDate;
        private Integer applicationsCount = 0;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder authorId(String authorId) {
            this.authorId = authorId;
            return this;
        }

        public Builder authorName(String authorName) {
            this.authorName = authorName;
            return this;
        }

        public Builder authorAvatarUrl(String authorAvatarUrl) {
            this.authorAvatarUrl = authorAvatarUrl;
            return this;
        }

        public Builder type(String type) {
            this.type = type != null ? type.toLowerCase() : "post";
            return this;
        }

        public Builder type(PostType type) {
            this.type = type != null ? type.toValue() : "post";
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder attachments(List<MediaFileReference> attachments) {
            this.attachments = attachments != null ? attachments : new ArrayList<>();
            return this;
        }

        public Builder mediaUrls(List<String> mediaUrls) {
            this.mediaUrls = mediaUrls != null ? mediaUrls : new ArrayList<>();
            return this;
        }

        public Builder visibility(PostVisibility visibility) {
            this.visibility = visibility != null ? visibility : PostVisibility.PUBLIC;
            return this;
        }

        public Builder reportCount(int reportCount) {
            this.reportCount = reportCount;
            return this;
        }

        public Builder isSpam(boolean isSpam) {
            this.isSpam = isSpam;
            return this;
        }

        public Builder employer(String employer) {
            this.employer = employer;
            return this;
        }

        public Builder compensation(String compensation) {
            this.compensation = compensation;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder expiryDate(Instant expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public Builder applicationsCount(Integer applicationsCount) {
            this.applicationsCount = applicationsCount != null ? applicationsCount : 0;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Post build() {
            Post post = new Post();
            post.setId(this.id);
            post.setAuthorId(this.authorId);
            post.setAuthorName(this.authorName);
            post.setAuthorAvatarUrl(this.authorAvatarUrl);
            post.setType(this.type);
            post.setTitle(this.title);
            post.setContent(this.content);
            post.setAttachments(this.attachments);
            post.setMediaUrls(this.mediaUrls);
            post.setVisibility(this.visibility);
            post.setReportCount(this.reportCount);
            post.setSpam(this.isSpam);
            post.setEmployer(this.employer);
            post.setCompensation(this.compensation);
            post.setLocation(this.location);
            post.setExpiryDate(this.expiryDate);
            post.setApplicationsCount(this.applicationsCount);
            post.setCreatedAt(this.createdAt != null ? this.createdAt : Instant.now());
            post.setUpdatedAt(this.updatedAt != null ? this.updatedAt : Instant.now());
            return post;
        }
    }

    public Post() {
    }

    public Post(String id, String authorId, String authorName, String authorAvatarUrl, String type, String title,
                String content, List<MediaFileReference> attachments, List<String> mediaUrls, PostVisibility visibility,
                int reportCount, boolean isSpam, String employer, String compensation, String location,
                Instant expiryDate, Integer applicationsCount, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.type = type;
        this.title = title;
        this.content = content;
        this.attachments = attachments;
        this.mediaUrls = mediaUrls;
        this.visibility = visibility;
        this.reportCount = reportCount;
        this.isSpam = isSpam;
        this.employer = employer;
        this.compensation = compensation;
        this.location = location;
        this.expiryDate = expiryDate;
        this.applicationsCount = applicationsCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public void setAuthorAvatarUrl(String authorAvatarUrl) { this.authorAvatarUrl = authorAvatarUrl; }

    public String getType() { return type; }

    public void setType(PostType postType) {
        this.type = postType != null ? postType.toValue() : "post";
    }

    public void setType(String type) {
        this.type = type != null ? type.toLowerCase() : "post";
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<MediaFileReference> getAttachments() { return attachments; }
    public void setAttachments(List<MediaFileReference> attachments) { this.attachments = attachments; }

    public List<String> getMediaUrls() { return mediaUrls; }
    public void setMediaUrls(List<String> mediaUrls) { this.mediaUrls = mediaUrls; }

    public PostVisibility getVisibility() { return visibility; }
    public void setVisibility(PostVisibility visibility) { this.visibility = visibility; }

    public int getReportCount() { return reportCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }

    public boolean isSpam() { return isSpam; }
    public void setSpam(boolean spam) { isSpam = spam; }

    public String getEmployer() { return employer; }
    public void setEmployer(String employer) { this.employer = employer; }

    public String getCompensation() { return compensation; }
    public void setCompensation(String compensation) { this.compensation = compensation; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Instant getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }

    public Integer getApplicationsCount() { return applicationsCount; }
    public void setApplicationsCount(Integer applicationsCount) { this.applicationsCount = applicationsCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
