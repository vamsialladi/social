package com.truehunt.social.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "user_profile_relationships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"owner_user_id", "target_profile_id"})
)
public class UserProfileRelationshipEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private String ownerUserId;

    @Column(name = "target_profile_id", nullable = false)
    private String targetProfileId;

    @Column(nullable = false)
    private boolean connected;

    @Column(nullable = false)
    private boolean following;

    public Long getId() {
        return id;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getTargetProfileId() {
        return targetProfileId;
    }

    public void setTargetProfileId(String targetProfileId) {
        this.targetProfileId = targetProfileId;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isFollowing() {
        return following;
    }

    public void setFollowing(boolean following) {
        this.following = following;
    }
}
