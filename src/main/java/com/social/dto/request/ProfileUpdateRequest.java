package com.social.dto.request;

import com.social.dto.response.MediaFileReferenceDto;
import com.social.entity.ProfileVisibility;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ProfileUpdateRequest {

    @Size(max = 128, message = "Name must not exceed 128 characters")
    private String name;

    @Size(max = 32, message = "Phone must not exceed 32 characters")
    private String phone;

    @Email(message = "Invalid email format")
    @Size(max = 128, message = "Email must not exceed 128 characters")
    private String email;

    @Size(max = 1000, message = "Experience must not exceed 1000 characters")
    private String experience;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    private List<String> languages;

    @Size(max = 128, message = "Other language must not exceed 128 characters")
    private String otherLanguage;

    private List<String> talents;

    private List<String> workTypes;

    private String profileImageKey;

    private String coverImageKey;

    private List<MediaFileReferenceDto> demoVideos;

    private List<String> videoUrls;

    private ProfileVisibility visibility;

    public ProfileUpdateRequest() {
    }

    public ProfileUpdateRequest(String name, String phone, String email, String experience,
                                String address, List<String> languages, String otherLanguage,
                                List<String> talents, List<String> workTypes, String profileImageKey,
                                String coverImageKey, List<MediaFileReferenceDto> demoVideos,
                                List<String> videoUrls, ProfileVisibility visibility) {
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
        this.coverImageKey = coverImageKey;
        this.demoVideos = demoVideos;
        this.videoUrls = videoUrls;
        this.visibility = visibility;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
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
        private String coverImageKey;
        private List<MediaFileReferenceDto> demoVideos;
        private List<String> videoUrls;
        private ProfileVisibility visibility;

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

        public ProfileUpdateRequest build() {
            return new ProfileUpdateRequest(name, phone, email, experience, address,
                    languages, otherLanguage, talents, workTypes, profileImageKey,
                    coverImageKey, demoVideos, videoUrls, visibility);
        }
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
}
