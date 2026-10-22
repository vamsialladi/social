package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.social.entity.Profile;
import com.social.entity.ProfileVisibility;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponse {

    @JsonProperty("_id")
    private String _id;

    @JsonProperty("id")
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
    private List<String> languages;
    private String otherLanguage;
    private List<String> talents;
    private List<String> workTypes;
    private String profileImageKey;
    private String profileImageUrl;
    private String avatarUrl;
    private String coverImageKey;
    private String coverImageUrl;
    private List<MediaFileReferenceDto> demoVideos;
    private List<String> videoUrls;
    private ProfileVisibility visibility;
    private Instant createdAt;
    private Instant updatedAt;

    public ProfileResponse() {
    }

    public ProfileResponse(String id, String userId, String displayName, String bio, String avatarUrl,
                           String headline, String location, Instant createdAt, Instant updatedAt) {
        this._id = id;
        this.id = id;
        this.userId = userId;
        this.displayName = displayName;
        this.name = displayName;
        this.bio = bio;
        this.experience = bio;
        this.avatarUrl = avatarUrl;
        this.profileImageUrl = avatarUrl;
        this.headline = headline;
        this.location = location;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public ProfileResponse(String id, String userId, String name, String phone, String email,
                           String experience, String address, List<String> languages, String otherLanguage,
                           List<String> talents, List<String> workTypes, String profileImageKey,
                           String profileImageUrl, String coverImageKey, String coverImageUrl,
                           List<MediaFileReferenceDto> demoVideos, List<String> videoUrls,
                           ProfileVisibility visibility, Instant createdAt, Instant updatedAt) {
        this._id = id;
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.displayName = name;
        this.phone = phone;
        this.email = email;
        this.experience = experience;
        this.bio = experience;
        this.address = address;
        this.languages = languages;
        this.otherLanguage = otherLanguage;
        this.talents = talents;
        this.workTypes = workTypes;
        this.profileImageKey = profileImageKey;
        this.profileImageUrl = profileImageUrl;
        this.avatarUrl = profileImageUrl;
        this.coverImageKey = coverImageKey;
        this.coverImageUrl = coverImageUrl;
        this.demoVideos = demoVideos;
        this.videoUrls = videoUrls;
        this.visibility = visibility;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProfileResponse fromEntity(Profile profile) {
        if (profile == null) {
            return null;
        }
        return fromEntity(profile, true, null);
    }

    public static ProfileResponse fromEntity(Profile profile, boolean isAuthorized, String mediaBaseUrl) {
        if (profile == null) {
            return null;
        }

        List<MediaFileReferenceDto> demoVideoDtos = new ArrayList<>();
        if (profile.getDemoVideos() != null) {
            demoVideoDtos = profile.getDemoVideos().stream()
                    .map(MediaFileReferenceDto::fromEntity)
                    .toList();
        }

        String profileImageUrl = null;
        if (StringUtils.hasText(profile.getProfileImageKey()) && StringUtils.hasText(mediaBaseUrl)) {
            profileImageUrl = mediaBaseUrl.endsWith("/")
                    ? mediaBaseUrl + profile.getProfileImageKey()
                    : mediaBaseUrl + "/" + profile.getProfileImageKey();
        } else if (StringUtils.hasText(profile.getAvatarUrl())) {
            profileImageUrl = profile.getAvatarUrl();
        }

        String coverImageUrl = null;
        if (StringUtils.hasText(profile.getCoverImageKey()) && StringUtils.hasText(mediaBaseUrl)) {
            coverImageUrl = mediaBaseUrl.endsWith("/")
                    ? mediaBaseUrl + profile.getCoverImageKey()
                    : mediaBaseUrl + "/" + profile.getCoverImageKey();
        }

        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .name(profile.getName())
                .displayName(profile.getDisplayName())
                .phone(isAuthorized ? profile.getPhone() : null)
                .email(isAuthorized ? profile.getEmail() : null)
                .experience(profile.getExperience())
                .bio(profile.getBio())
                .headline(profile.getHeadline())
                .location(profile.getLocation())
                .address(profile.getAddress())
                .languages(profile.getLanguages() != null ? new ArrayList<>(profile.getLanguages()) : new ArrayList<>())
                .otherLanguage(profile.getOtherLanguage())
                .talents(profile.getTalents() != null ? new ArrayList<>(profile.getTalents()) : new ArrayList<>())
                .workTypes(profile.getWorkTypes() != null ? new ArrayList<>(profile.getWorkTypes()) : new ArrayList<>())
                .profileImageKey(profile.getProfileImageKey())
                .profileImageUrl(profileImageUrl)
                .avatarUrl(profileImageUrl)
                .coverImageKey(profile.getCoverImageKey())
                .coverImageUrl(coverImageUrl)
                .demoVideos(demoVideoDtos)
                .videoUrls(profile.getVideoUrls() != null ? new ArrayList<>(profile.getVideoUrls()) : new ArrayList<>())
                .visibility(profile.getVisibility())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
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
        private List<String> languages;
        private String otherLanguage;
        private List<String> talents;
        private List<String> workTypes;
        private String profileImageKey;
        private String profileImageUrl;
        private String avatarUrl;
        private String coverImageKey;
        private String coverImageUrl;
        private List<MediaFileReferenceDto> demoVideos;
        private List<String> videoUrls;
        private ProfileVisibility visibility;
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

        public Builder profileImageUrl(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
            if (this.avatarUrl == null) {
                this.avatarUrl = profileImageUrl;
            }
            return this;
        }

        public Builder avatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            if (this.profileImageUrl == null) {
                this.profileImageUrl = avatarUrl;
            }
            return this;
        }

        public Builder coverImageKey(String coverImageKey) {
            this.coverImageKey = coverImageKey;
            return this;
        }

        public Builder coverImageUrl(String coverImageUrl) {
            this.coverImageUrl = coverImageUrl;
            return this;
        }

        public Builder demoVideos(List<MediaFileReferenceDto> demoVideos) {
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

        public ProfileResponse build() {
            ProfileResponse resp = new ProfileResponse(id, userId, name, phone, email, experience, address,
                    languages, otherLanguage, talents, workTypes, profileImageKey,
                    profileImageUrl, coverImageKey, coverImageUrl, demoVideos, videoUrls,
                    visibility, createdAt, updatedAt);
            resp.setDisplayName(this.displayName != null ? this.displayName : this.name);
            resp.setBio(this.bio != null ? this.bio : this.experience);
            resp.setHeadline(this.headline);
            resp.setLocation(this.location);
            resp.setAvatarUrl(this.avatarUrl != null ? this.avatarUrl : this.profileImageUrl);
            return resp;
        }
    }

    public String get_id() {
        return _id != null ? _id : id;
    }

    public void set_id(String _id) {
        this._id = _id;
        this.id = _id;
    }

    public String getId() {
        return id != null ? id : _id;
    }

    public void setId(String id) {
        this.id = id;
        this._id = id;
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

    public String getProfileImageUrl() {
        return profileImageUrl != null ? profileImageUrl : avatarUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
        if (this.avatarUrl == null) {
            this.avatarUrl = profileImageUrl;
        }
    }

    public String getAvatarUrl() {
        return avatarUrl != null ? avatarUrl : profileImageUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        if (this.profileImageUrl == null) {
            this.profileImageUrl = avatarUrl;
        }
    }

    public String getCoverImageKey() {
        return coverImageKey;
    }

    public void setCoverImageKey(String coverImageKey) {
        this.coverImageKey = coverImageKey;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public List<MediaFileReferenceDto> getDemoVideos() {
        return demoVideos;
    }

    public void setDemoVideos(List<MediaFileReferenceDto> demoVideos) {
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
