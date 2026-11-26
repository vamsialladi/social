package com.truehunt.social.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "connection_groups")
public class ConnectionGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "group_type", length = 20)
    private String groupType = "GROUP";

    @Lob
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "connection_group_members", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "member_profile_id", nullable = false)
    @OrderColumn(name = "member_order")
    private List<String> memberProfileIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "connection_group_followers", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "follower_user_id", nullable = false)
    private List<String> followerUserIds = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupType() {
        return groupType == null ? "GROUP" : groupType;
    }

    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<String> getMemberProfileIds() {
        return memberProfileIds;
    }

    public void setMemberProfileIds(List<String> memberProfileIds) {
        this.memberProfileIds = memberProfileIds == null ? new ArrayList<>() : new ArrayList<>(memberProfileIds);
    }

    public List<String> getFollowerUserIds() {
        return followerUserIds;
    }

    public void setFollowerUserIds(List<String> followerUserIds) {
        this.followerUserIds = followerUserIds == null ? new ArrayList<>() : new ArrayList<>(followerUserIds);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
