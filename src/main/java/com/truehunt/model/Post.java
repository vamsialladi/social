package com.truehunt.model;

import com.truehunt.model.converter.MediaAttachmentListConverter;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_post_type_created", columnList = "type, created_at"),
    @Index(name = "idx_post_author", columnList = "author_id"),
    @Index(name = "idx_post_created", columnList = "created_at")
})
public class Post {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "author_id", length = 36, nullable = false)
    private String authorId;

    @Column(name = "type", length = 30, nullable = false)
    private String type; // "post", "article", "opportunity"

    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Convert(converter = MediaAttachmentListConverter.class)
    @Column(name = "attachments", columnDefinition = "TEXT")
    private List<MediaAttachment> attachments = new ArrayList<>();

    @Column(name = "visibility", length = 30, nullable = false)
    private String visibility = "public";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Post() {}

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.attachments == null) this.attachments = new ArrayList<>();
        if (this.visibility == null) this.visibility = "public";
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
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

    public List<MediaAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MediaAttachment> attachments) {
        this.attachments = attachments;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
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
