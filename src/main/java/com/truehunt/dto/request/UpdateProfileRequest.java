package com.truehunt.dto.request;

import com.truehunt.model.MediaAttachment;
import java.util.List;

public class UpdateProfileRequest {
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
    private List<MediaAttachment> demoVideos;
    private List<String> videoUrls;
    private String visibility;

    public UpdateProfileRequest() {}

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
}
