package com.truehunt.social.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "saved_posts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_saved_post_user_post",
                columnNames = {"user_email", "post_id"}
        )
)
public class SavedPostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String snapshotJson;

    private LocalDateTime savedAt = LocalDateTime.now();

    public SavedPostEntity() {}

    public SavedPostEntity(String userEmail, Long postId, String snapshotJson) {
        this.userEmail = userEmail;
        this.postId = postId;
        this.snapshotJson = snapshotJson;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }

    public LocalDateTime getSavedAt() { return savedAt; }
    public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }
}
