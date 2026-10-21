package com.social.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_post_type_created", columnList = "type, created_at DESC"),
        @Index(name = "idx_post_author", columnList = "author_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "author_id", length = 36, nullable = false)
    private String authorId;

    @Column(name = "author_name", length = 150)
    private String authorName;

    @Column(name = "author_avatar_url", length = 500)
    private String authorAvatarUrl;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "type", length = 50, nullable = false)
    @Builder.Default
    private String type = "post";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_media_urls", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "media_url", length = 500)
    @Builder.Default
    private List<String> mediaUrls = new ArrayList<>();

    // Structured Opportunity fields
    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "employer", length = 150)
    private String employer;

    @Column(name = "compensation", length = 100)
    private String compensation;

    @Column(name = "location", length = 150)
    private String location;

    @Column(name = "expiry_date")
    private Instant expiryDate;

    @Column(name = "applications_count", nullable = false)
    @Builder.Default
    private Integer applicationsCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        this.updatedAt = Instant.now();
        if (this.applicationsCount == null) {
            this.applicationsCount = 0;
        }
        if (this.mediaUrls == null) {
            this.mediaUrls = new ArrayList<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
