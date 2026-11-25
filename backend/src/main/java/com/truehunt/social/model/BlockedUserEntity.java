package com.truehunt.social.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(
        name = "blocked_users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_blocker_blocked_identity",
                columnNames = {"blocker_email", "blocked_identity"}
        )
)
public class BlockedUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blocker_email", nullable = false, length = 255)
    private String blockerEmail;

    @Column(name = "blocked_identity", nullable = false, length = 255)
    private String blockedIdentity;

    @Column(name = "blocked_email", length = 255)
    private String blockedEmail;

    @Column(name = "blocked_name", length = 255)
    private String blockedName;

    @Lob
    @Column(name = "blocked_photo", columnDefinition = "CLOB")
    private String blockedPhoto;

    @Column(length = 500)
    private String reason;

    private LocalDateTime createdAt = LocalDateTime.now();

    public BlockedUserEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBlockerEmail() { return blockerEmail; }
    public void setBlockerEmail(String blockerEmail) { this.blockerEmail = blockerEmail; }

    public String getBlockedIdentity() { return blockedIdentity; }
    public void setBlockedIdentity(String blockedIdentity) { this.blockedIdentity = blockedIdentity; }

    public String getBlockedEmail() { return blockedEmail; }
    public void setBlockedEmail(String blockedEmail) { this.blockedEmail = blockedEmail; }

    public String getBlockedName() { return blockedName; }
    public void setBlockedName(String blockedName) { this.blockedName = blockedName; }

    public String getBlockedPhoto() { return blockedPhoto; }
    public void setBlockedPhoto(String blockedPhoto) { this.blockedPhoto = blockedPhoto; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> toDto() {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", id);
        dto.put("identity", blockedIdentity != null ? blockedIdentity : "");
        dto.put("name", blockedName != null ? blockedName : "");
        dto.put("email", blockedEmail != null ? blockedEmail : "");
        dto.put("photo", blockedPhoto != null ? blockedPhoto : "");
        dto.put("reason", reason != null ? reason : "");
        dto.put("createdAt", createdAt != null ? createdAt.toString() : "");
        return dto;
    }
}
