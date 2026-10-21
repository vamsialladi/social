package com.social.entity;

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

    @Column(name = "user_id", nullable = false, unique = true, length = 64)
    private String userId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "experience", length = 1000)
    private String experience;

    @Column(name = "address", length = 255)
    private String address;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_languages", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "language")
    private List<String> languages = new ArrayList<>();

    @Column(name = "other_language", length = 128)
    private String otherLanguage;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_talents", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "talent")
    private List<String> talents = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_work_types", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "work_type")
    private List<String> workTypes = new ArrayList<>();

    @Column(name = "profile_image_key", length = 255)
    private String profileImageKey;

    @Column(name = "cover_image_key", length = 255)
    private String coverImageKey;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_demo_videos", joinColumns = @JoinColumn(name = "profile_id"))
    private List<MediaFileReference> demoVideos = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_video_urls", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "video_url", length = 512)
    private List<String> videoUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private ProfileVisibility visibility = ProfileVisibility.PUBLIC;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Profile() {
    }

    public Profile(String id, String userId, String name, String phone, String email,
                   String experience, String address, List<String> languages, String otherLanguage,
                   List<String> talents, List<String> workTypes, String profileImageKey,
                   String coverImageKey, List<MediaFileReference> demoVideos, List<String> videoUrls,
                   ProfileVisibility visibility, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.experience = experience;
        this.address = address;
        this.languages = languages != null ? languages : new ArrayList<>();
        this.otherLanguage = otherLanguage;
        this.talents = talents != null ? talents : new ArrayList<>();
        this.workTypes = workTypes != null ? workTypes : new ArrayList<>();
        this.profileImageKey = profileImageKey;
        this.coverImageKey = coverImageKey;
        this.demoVideos = demoVideos != null ? demoVideos : new ArrayList<>();
        this.videoUrls = videoUrls != null ? videoUrls : new ArrayList<>();
        this.visibility = visibility != null ? visibility : ProfileVisibility.PUBLIC;
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
        private String userId;
        private String name;
        private String phone;
        private String email;
        private String experience;
        private String address;
        private List<String> languages = new ArrayList<>();
        private String otherLanguage;
        private List<String> talents = new ArrayList<>();
        private List<String> workTypes = new ArrayList<>();
        private String profileImageKey;
        private String coverImageKey;
        private List<MediaFileReference> demoVideos = new ArrayList<>();
        private List<String> videoUrls = new ArrayList<>();
        private ProfileVisibility visibility = ProfileVisibility.PUBLIC;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder experience(String experience) {
            this.experience = experience;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder languages(List<String> languages) {
            this.languages = languages;
            return this;
        }

        public Builder otherLanguage(String otherLanguage) {
            this.otherLanguage = otherLanguage;
            return this;
        }

        public Builder talents(List<String> talents) {
            this.talents = talents;
            return this;
        }

        public Builder workTypes(List<String> workTypes) {
            this.workTypes = workTypes;
            return this;
        }

        public Builder profileImageKey(String profileImageKey) {
            this.profileImageKey = profileImageKey;
            return this;
        }

        public Builder coverImageKey(String coverImageKey) {
            this.coverImageKey = coverImageKey;
            return this;
        }

        public Builder demoVideos(List<MediaFileReference> demoVideos) {
            this.demoVideos = demoVideos;
            return this;
        }

        public Builder videoUrls(List<String> videoUrls) {
            this.videoUrls = videoUrls;
            return this;
        }

        public Builder visibility(ProfileVisibility visibility) {
            this.visibility = visibility;
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

        public Profile build() {
            return new Profile(id, userId, name, phone, email, experience, address,
                    languages, otherLanguage, talents, workTypes, profileImageKey,
                    coverImageKey, demoVideos, videoUrls, visibility, createdAt, updatedAt);
        }
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

    public List<MediaFileReference> getDemoVideos() {
        return demoVideos;
    }

    public void setDemoVideos(List<MediaFileReference> demoVideos) {
        this.demoVideos = demoVideos;
    }

    public List<String> getVideoUrls() {
        return videoUrls;
    }

    public void setVideoUrls(List<String> videoUrls) {
        this.videoUrls = videoUrls;
    }

    public ProfileVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(ProfileVisibility visibility) {
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
