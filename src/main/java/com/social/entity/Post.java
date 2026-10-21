package com.social.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_post_created_at", columnList = "created_at"),
        @Index(name = "idx_post_author_id", columnList = "author_id"),
        @Index(name = "idx_post_type", columnList = "type"),
        @Index(name = "idx_post_visibility", columnList = "visibility")
})
public class Post {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "author_id", nullable = false, length = 64)
    private String authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PostType type;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "content", nullable = false, length = 10000)
    private String content;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_attachments", joinColumns = @JoinColumn(name = "post_id"))
    private List<MediaFileReference> attachments = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private PostVisibility visibility = PostVisibility.PUBLIC;

    @Column(name = "report_count", nullable = false)
    private int reportCount = 0;

    @Column(name = "is_spam", nullable = false)
    private boolean isSpam = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Post() {
    }

    public Post(String id, String authorId, PostType type, String title, String content,
                List<MediaFileReference> attachments, PostVisibility visibility, int reportCount,
                boolean isSpam, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.authorId = authorId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.attachments = attachments != null ? attachments : new ArrayList<>();
        this.visibility = visibility != null ? visibility : PostVisibility.PUBLIC;
        this.reportCount = reportCount;
        this.isSpam = isSpam;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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
        private PostType type;
        private String title;
        private String content;
        private List<MediaFileReference> attachments = new ArrayList<>();
        private PostVisibility visibility = PostVisibility.PUBLIC;
        private int reportCount = 0;
        private boolean isSpam = false;
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

        public Builder type(PostType type) {
            this.type = type;
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
            this.attachments = attachments;
            return this;
        }

        public Builder visibility(PostVisibility visibility) {
            this.visibility = visibility;
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

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Post build() {
            return new Post(id, authorId, type, title, content, attachments, visibility, reportCount, isSpam, createdAt, updatedAt);
        }
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

    public PostType getType() {
        return type;
    }

    public void setType(PostType type) {
        this.type = type;
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

    public List<MediaFileReference> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MediaFileReference> attachments) {
        this.attachments = attachments;
    }

    public PostVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(PostVisibility visibility) {
        this.visibility = visibility;
    }

    public int getReportCount() {
        return reportCount;
    }

    public void setReportCount(int reportCount) {
        this.reportCount = reportCount;
    }

    public boolean isSpam() {
        return isSpam;
    }

    public void setSpam(boolean spam) {
        isSpam = spam;
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
