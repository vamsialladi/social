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

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "group_type", length = 20)
    private String groupType = "GROUP";

    @Column(name = "owner_name", length = 120)
    private String ownerName;

    @Column(name = "public_url", length = 63)
    private String publicUrl;

    @Column(length = 255)
    private String website;

    @Column(length = 100)
    private String industry;

    @Column(length = 120)
    private String tagline;

    @Lob
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Lob
    @Column(name = "banner_url", columnDefinition = "TEXT")
    private String bannerUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "connection_group_members", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "member_profile_id", nullable = false)
    @OrderColumn(name = "member_order")
    private List<String> memberProfileIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "connection_group_followers", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "follower_user_id", nullable = false)
    private List<String> followerUserIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "connection_group_subscribers", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "subscriber_user_id", nullable = false)
    @OrderColumn(name = "subscriber_order")
    private List<String> subscriberUserIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "organization_page_admins", joinColumns = @JoinColumn(name = "page_id"))
    @Column(name = "admin_user_id", nullable = false)
    @OrderColumn(name = "admin_order")
    private List<String> pageAdminUserIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "organization_page_blocked_members", joinColumns = @JoinColumn(name = "page_id"))
    @Column(name = "blocked_user_id", nullable = false)
    @OrderColumn(name = "blocked_order")
    private List<String> pageBlockedUserIds = new ArrayList<>();

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

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
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

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
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

    public List<String> getSubscriberUserIds() {
        return subscriberUserIds;
    }

    public void setSubscriberUserIds(List<String> subscriberUserIds) {
        this.subscriberUserIds = subscriberUserIds == null ? new ArrayList<>() : new ArrayList<>(subscriberUserIds);
    }

    public List<String> getPageAdminUserIds() {
        return pageAdminUserIds;
    }

    public void setPageAdminUserIds(List<String> pageAdminUserIds) {
        this.pageAdminUserIds = pageAdminUserIds == null ? new ArrayList<>() : new ArrayList<>(pageAdminUserIds);
    }

    public List<String> getPageBlockedUserIds() {
        return pageBlockedUserIds;
    }

    public void setPageBlockedUserIds(List<String> pageBlockedUserIds) {
        this.pageBlockedUserIds = pageBlockedUserIds == null ? new ArrayList<>() : new ArrayList<>(pageBlockedUserIds);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
