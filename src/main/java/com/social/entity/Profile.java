package com.social.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("id")
    private String id;

    @Column(name = "user_id", nullable = false, unique = true, length = 64)
    private String userId;

    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "experience", length = 1000)
    private String experience;

    @Column(name = "bio", length = 1000)
    private String bio;

    @Column(name = "headline", length = 255)
    private String headline;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "avatar_url", length = 1024)
    private String avatarUrl;

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

    public Profile(String id, String userId, String displayName, String bio, String avatarUrl,
                   String headline, String location, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.displayName = displayName;
        this.name = displayName;
        this.bio = bio;
        this.experience = bio;
        this.avatarUrl = avatarUrl;
        this.headline = headline;
        this.location = location;
        this.visibility = ProfileVisibility.PUBLIC;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public Profile(String id, String userId, String name, String phone, String email,
                   String experience, String address, List<String> languages, String otherLanguage,
                   List<String> talents, List<String> workTypes, String profileImageKey,
                   String coverImageKey, List<MediaFileReference> demoVideos, List<String> videoUrls,
                   ProfileVisibility visibility, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.displayName = name;
        this.phone = phone;
        this.email = email;
        this.experience = experience;
        this.bio = experience;
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
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
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
        if (this.visibility == null) {
            this.visibility = ProfileVisibility.PUBLIC;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @JsonProperty("_id")
    public String get_id() {
        return id;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String userId;
        private String name;
        private String displayName;
        private String phone;
        private String email;
        private String experience;
        private String bio;
        private String headline;
        private String location;
        private String address;
        private String avatarUrl;
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
            if (this.displayName == null) {
                this.displayName = name;
            }
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            if (this.name == null) {
                this.name = displayName;
            }
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
            if (this.bio == null) {
                this.bio = experience;
            }
            return this;
        }

        public Builder bio(String bio) {
            this.bio = bio;
            if (this.experience == null) {
                this.experience = bio;
            }
            return this;
        }

        public Builder headline(String headline) {
            this.headline = headline;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder avatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }

        public Builder languages(List<String> languages) {
            this.languages = languages != null ? languages : new ArrayList<>();
            return this;
        }

        public Builder otherLanguage(String otherLanguage) {
            this.otherLanguage = otherLanguage;
            return this;
        }

        public Builder talents(List<String> talents) {
            this.talents = talents != null ? talents : new ArrayList<>();
            return this;
        }

        public Builder workTypes(List<String> workTypes) {
            this.workTypes = workTypes != null ? workTypes : new ArrayList<>();
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
            this.demoVideos = demoVideos != null ? demoVideos : new ArrayList<>();
            return this;
        }

        public Builder videoUrls(List<String> videoUrls) {
            this.videoUrls = videoUrls != null ? videoUrls : new ArrayList<>();
            return this;
        }

        public Builder visibility(ProfileVisibility visibility) {
            this.visibility = visibility != null ? visibility : ProfileVisibility.PUBLIC;
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
            Profile profile = new Profile();
            profile.id = this.id;
            profile.userId = this.userId;
            profile.name = this.name != null ? this.name : this.displayName;
            profile.displayName = this.displayName != null ? this.displayName : this.name;
            profile.phone = this.phone;
            profile.email = this.email;
            profile.experience = this.experience != null ? this.experience : this.bio;
            profile.bio = this.bio != null ? this.bio : this.experience;
            profile.headline = this.headline;
            profile.location = this.location;
            profile.address = this.address;
            profile.avatarUrl = this.avatarUrl;
            profile.languages = this.languages != null ? this.languages : new ArrayList<>();
            profile.otherLanguage = this.otherLanguage;
            profile.talents = this.talents != null ? this.talents : new ArrayList<>();
            profile.workTypes = this.workTypes != null ? this.workTypes : new ArrayList<>();
            profile.profileImageKey = this.profileImageKey;
            profile.coverImageKey = this.coverImageKey;
            profile.demoVideos = this.demoVideos != null ? this.demoVideos : new ArrayList<>();
            profile.videoUrls = this.videoUrls != null ? this.videoUrls : new ArrayList<>();
            profile.visibility = this.visibility != null ? this.visibility : ProfileVisibility.PUBLIC;
            profile.createdAt = this.createdAt;
            profile.updatedAt = this.updatedAt;
            return profile;
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
        return name != null ? name : displayName;
    }

    public void setName(String name) {
        this.name = name;
        if (this.displayName == null) {
            this.displayName = name;
        }
    }

    public String getDisplayName() {
        return displayName != null ? displayName : name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        if (this.name == null) {
            this.name = displayName;
        }
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
        return experience != null ? experience : bio;
    }

    public void setExperience(String experience) {
        this.experience = experience;
        if (this.bio == null) {
            this.bio = experience;
        }
    }

    public String getBio() {
        return bio != null ? bio : experience;
    }

    public void setBio(String bio) {
        this.bio = bio;
        if (this.experience == null) {
            this.experience = bio;
        }
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
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
