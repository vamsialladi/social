package com.truehunt.social.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "hidden_content",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_hidden_content_user_target",
                columnNames = {"user_email", "target_type", "target_id"}
        )
)
public class HiddenContentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(name = "target_type", nullable = false, length = 32)
    private String targetType; // POST, AUTHOR, INVESTOR_LISTING

    @Column(name = "target_id", nullable = false, length = 255)
    private String targetId;

    private LocalDateTime createdAt = LocalDateTime.now();

    public HiddenContentEntity() {}

    public HiddenContentEntity(String userEmail, String targetType, String targetId) {
        this.userEmail = userEmail;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
