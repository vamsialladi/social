package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.social.entity.Profile;
import com.social.entity.ProfileVisibility;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponse {

    private String id;
    private String userId;
    private String name;
    private String phone;
    private String email;
    private String experience;
    private String address;
    private List<String> languages;
    private String otherLanguage;
    private List<String> talents;
    private List<String> workTypes;
    private String profileImageKey;
    private String profileImageUrl;
    private String coverImageKey;
    private String coverImageUrl;
    private List<MediaFileReferenceDto> demoVideos;
    private List<String> videoUrls;
    private ProfileVisibility visibility;
    private Instant createdAt;
    private Instant updatedAt;

    public ProfileResponse() {
    }

    public ProfileResponse(String id, String userId, String name, String phone, String email,
                           String experience, String address, List<String> languages, String otherLanguage,
                           List<String> talents, List<String> workTypes, String profileImageKey,
                           String profileImageUrl, String coverImageKey, String coverImageUrl,
                           List<MediaFileReferenceDto> demoVideos, List<String> videoUrls,
                           ProfileVisibility visibility, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.experience = experience;
        this.address = address;
        this.languages = languages;
        this.otherLanguage = otherLanguage;
        this.talents = talents;
        this.workTypes = workTypes;
        this.profileImageKey = profileImageKey;
        this.profileImageUrl = profileImageUrl;
        this.coverImageKey = coverImageKey;
        this.coverImageUrl = coverImageUrl;
        this.demoVideos = demoVideos;
        this.videoUrls = videoUrls;
        this.visibility = visibility;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
                .phone(isAuthorized ? profile.getPhone() : null)
                .email(isAuthorized ? profile.getEmail() : null)
                .experience(profile.getExperience())
                .address(profile.getAddress())
                .languages(profile.getLanguages() != null ? new ArrayList<>(profile.getLanguages()) : new ArrayList<>())
                .otherLanguage(profile.getOtherLanguage())
                .talents(profile.getTalents() != null ? new ArrayList<>(profile.getTalents()) : new ArrayList<>())
                .workTypes(profile.getWorkTypes() != null ? new ArrayList<>(profile.getWorkTypes()) : new ArrayList<>())
                .profileImageKey(profile.getProfileImageKey())
                .profileImageUrl(profileImageUrl)
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
        private String phone;
        private String email;
        private String experience;
        private String address;
        private List<String> languages;
        private String otherLanguage;
        private List<String> talents;
        private List<String> workTypes;
        private String profileImageKey;
        private String profileImageUrl;
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

        public Builder profileImageUrl(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
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
            return new ProfileResponse(id, userId, name, phone, email, experience, address,
                    languages, otherLanguage, talents, workTypes, profileImageKey,
                    profileImageUrl, coverImageKey, coverImageUrl, demoVideos, videoUrls,
                    visibility, createdAt, updatedAt);
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

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
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
