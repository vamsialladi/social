package com.truehunt.model;

import com.truehunt.model.converter.MediaAttachmentListConverter;
import com.truehunt.model.converter.StringListConverter;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "profiles", indexes = {
    @Index(name = "idx_profile_user_id", columnList = "user_id", unique = true)
})
public class Profile {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false, unique = true)
    private String userId;

    @Column(name = "name")
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "experience")
    private String experience;

    @Column(name = "address", length = 1000)
    private String address;

    @Convert(converter = StringListConverter.class)
    @Column(name = "languages", columnDefinition = "TEXT")
    private List<String> languages = new ArrayList<>();

    @Column(name = "other_language")
    private String otherLanguage;

    @Convert(converter = StringListConverter.class)
    @Column(name = "talents", columnDefinition = "TEXT")
    private List<String> talents = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "work_types", columnDefinition = "TEXT")
    private List<String> workTypes = new ArrayList<>();

    @Column(name = "profile_image_key")
    private String profileImageKey;

    @Column(name = "cover_image_key")
    private String coverImageKey;

    @Convert(converter = MediaAttachmentListConverter.class)
    @Column(name = "demo_videos", columnDefinition = "TEXT")
    private List<MediaAttachment> demoVideos = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "video_urls", columnDefinition = "TEXT")
    private List<String> videoUrls = new ArrayList<>();

    @Column(name = "visibility", nullable = false, length = 30)
    private String visibility = "public";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Profile() {}

    public Profile(String userId) {
        this.userId = userId;
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.languages == null) this.languages = new ArrayList<>();
        if (this.talents == null) this.talents = new ArrayList<>();
        if (this.workTypes == null) this.workTypes = new ArrayList<>();
        if (this.demoVideos == null) this.demoVideos = new ArrayList<>();
        if (this.videoUrls == null) this.videoUrls = new ArrayList<>();
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public String getOtherLanguage() {
        return otherLanguage;
    }

    public void setOtherLanguage(String otherLanguage) {
        this.otherLanguage = otherLanguage;
    }

    public List<String> getTalents() {
        return talents;
    }

    public void setTalents(List<String> talents) {
        this.talents = talents;
    }

    public List<String> getWorkTypes() {
        return workTypes;
    }

    public void setWorkTypes(List<String> workTypes) {
        this.workTypes = workTypes;
    }

    public String getProfileImageKey() {
        return profileImageKey;
    }

    public void setProfileImageKey(String profileImageKey) {
        this.profileImageKey = profileImageKey;
    }

    public String getCoverImageKey() {
        return coverImageKey;
    }

    public void setCoverImageKey(String coverImageKey) {
        this.coverImageKey = coverImageKey;
    }

    public List<MediaAttachment> getDemoVideos() {
        return demoVideos;
    }

    public void setDemoVideos(List<MediaAttachment> demoVideos) {
        this.demoVideos = demoVideos;
    }

    public List<String> getVideoUrls() {
        return videoUrls;
    }

    public void setVideoUrls(List<String> videoUrls) {
        this.videoUrls = videoUrls;
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
