package com.truehunt.social.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfileDto {
    private String id;
    private String userId;
    private String referId;
    private String name;
    private String headline;
    private String role;
    private String category;
    private String company;
    private String industry;
    private String location;
    private String bio;
    private String avatar;
    private String profileImageUrl;
    private String coverImage;
    private String coverImageUrl;
    private String availability;
    private Boolean verified = false;
    private Integer connectionsCount = 0;
    private Integer followersCount = 0;
    private String websiteUrl;
    private String showreel;

    private String phone;
    private String email;
    private String address;
    private String dateOfBirth;
    private String experience;
    private String primaryCraft;
    private String subCraft;
    private String height;
    private String weight;
    private String bodyBuild;
    private String gender;
    private String facialHair;
    private String skinTone;
    private String otherLanguage;

    private Boolean isOwner = false;

    private List<String> skills = new ArrayList<>();
    private List<String> languages = new ArrayList<>();
    private List<String> secondaryCrafts = new ArrayList<>();
    private List<String> secondarySubCrafts = new ArrayList<>();
    private List<String> workTypes = new ArrayList<>();
    private List<String> videoUrls = new ArrayList<>();
    private List<Map<String, Object>> videos = new ArrayList<>();

    private List<ExperienceDto> experiences = new ArrayList<>();
    private List<EducationDto> educations = new ArrayList<>();
    private List<ProjectDto> projects = new ArrayList<>();
    private List<Map<String, Object>> posts = new ArrayList<>();
    private List<Map<String, Object>> credits = new ArrayList<>();

    public ProfileDto() {}

    public String getId() {
        return id != null ? id : userId;
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

    public String getReferId() {
        return referId != null ? referId : (userId != null ? userId : id);
    }

    public void setReferId(String referId) {
        this.referId = referId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getRole() {
        return role != null ? role : primaryCraft;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCategory() {
        return category != null ? category : primaryCraft;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getLocation() {
        return location != null ? location : address;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatar() {
        return avatar != null ? avatar : profileImageUrl;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
        this.profileImageUrl = avatar;
    }

    public String getProfileImageUrl() {
        return profileImageUrl != null ? profileImageUrl : avatar;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
        this.avatar = profileImageUrl;
    }

    public String getCoverImage() {
        return coverImage != null ? coverImage : coverImageUrl;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
        this.coverImageUrl = coverImage;
    }

    public String getCoverImageUrl() {
        return coverImageUrl != null ? coverImageUrl : coverImage;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
        this.coverImage = coverImageUrl;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public Integer getConnectionsCount() {
        return connectionsCount;
    }

    public void setConnectionsCount(Integer connectionsCount) {
        this.connectionsCount = connectionsCount;
    }

    public Integer getFollowersCount() {
        return followersCount;
    }

    public void setFollowersCount(Integer followersCount) {
        this.followersCount = followersCount;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getShowreel() {
        return showreel;
    }

    public void setShowreel(String showreel) {
        this.showreel = showreel;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getPrimaryCraft() {
        return primaryCraft;
    }

    public void setPrimaryCraft(String primaryCraft) {
        this.primaryCraft = primaryCraft;
    }

    public String getSubCraft() {
        return subCraft;
    }

    public void setSubCraft(String subCraft) {
        this.subCraft = subCraft;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getBodyBuild() {
        return bodyBuild;
    }

    public void setBodyBuild(String bodyBuild) {
        this.bodyBuild = bodyBuild;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getFacialHair() {
        return facialHair;
    }

    public void setFacialHair(String facialHair) {
        this.facialHair = facialHair;
    }

    public String getSkinTone() {
        return skinTone;
    }

    public void setSkinTone(String skinTone) {
        this.skinTone = skinTone;
    }

    public String getOtherLanguage() {
        return otherLanguage;
    }

    public void setOtherLanguage(String otherLanguage) {
        this.otherLanguage = otherLanguage;
    }

    public Boolean getIsOwner() {
        return isOwner;
    }

    public void setIsOwner(Boolean owner) {
        isOwner = owner;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public List<String> getSecondaryCrafts() {
        return secondaryCrafts;
    }

    public void setSecondaryCrafts(List<String> secondaryCrafts) {
        this.secondaryCrafts = secondaryCrafts;
    }

    public List<String> getSecondarySubCrafts() {
        return secondarySubCrafts;
    }

    public void setSecondarySubCrafts(List<String> secondarySubCrafts) {
        this.secondarySubCrafts = secondarySubCrafts;
    }

    public List<String> getWorkTypes() {
        return workTypes;
    }

    public void setWorkTypes(List<String> workTypes) {
        this.workTypes = workTypes;
    }

    public List<String> getVideoUrls() {
        return videoUrls;
    }

    public void setVideoUrls(List<String> videoUrls) {
        this.videoUrls = videoUrls;
    }

    public List<Map<String, Object>> getVideos() {
        return videos;
    }

    public void setVideos(List<Map<String, Object>> videos) {
        this.videos = videos;
    }

    public List<ExperienceDto> getExperiences() {
        return experiences;
    }

    public void setExperiences(List<ExperienceDto> experiences) {
        this.experiences = experiences;
    }

    public List<EducationDto> getEducations() {
        return educations;
    }

    public void setEducations(List<EducationDto> educations) {
        this.educations = educations;
    }

    public List<ProjectDto> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectDto> projects) {
        this.projects = projects;
    }

    public List<Map<String, Object>> getPosts() {
        return posts;
    }

    public void setPosts(List<Map<String, Object>> posts) {
        this.posts = posts;
    }

    public List<Map<String, Object>> getCredits() {
        return credits;
    }

    public void setCredits(List<Map<String, Object>> credits) {
        this.credits = credits;
    }
}
