package com.truehunt.social.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TalentDto {
    private String id;
    private String userId;
    private String email;
    private String name;
    private String role;
    private String category;
    @JsonAlias({"primary_craft", "craft"})
    private String primaryCraft;
    @JsonAlias({"subcraft", "sub_craft"})
    private String subCraft;
    private String location;
    private String experience;
    private String avatar;
    private String coverImage;
    private String bio;
    private List<String> skills = new ArrayList<>();
    private List<Map<String, Object>> credits = new ArrayList<>();
    private String showreel;
    private String availability;
    private Boolean verified = false;
    private Boolean isConnected = false;
    private Boolean connected = false;
    private Boolean isPending = false;
    private Boolean isOutgoingPending = false;
    private Boolean isIncomingPending = false;
    private String connectionStatus;
    private Integer connectionsCount = 0;
    private Integer followersCount = 0;
    private String headline;
    private String company;
    private String industry;
    private String websiteUrl;
    private List<ExperienceDto> experiences = new ArrayList<>();
    private List<EducationDto> educations = new ArrayList<>();
    private List<ProjectDto> projects = new ArrayList<>();
    private List<Map<String, Object>> posts = new ArrayList<>();

    public String getUserId() {
        return userId != null && !userId.isBlank() ? userId : id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getIsConnected() {
        return isConnected;
    }

    public void setIsConnected(Boolean isConnected) {
        this.isConnected = Boolean.TRUE.equals(isConnected);
        this.connected = Boolean.TRUE.equals(isConnected);
    }

    public Boolean getConnected() {
        return connected != null ? connected : isConnected;
    }

    public void setConnected(Boolean connected) {
        this.connected = Boolean.TRUE.equals(connected);
        this.isConnected = Boolean.TRUE.equals(connected);
    }

    public Boolean getIsPending() {
        return isPending;
    }

    public void setIsPending(Boolean isPending) {
        this.isPending = isPending;
    }

    public Boolean getIsOutgoingPending() {
        return isOutgoingPending;
    }

    public void setIsOutgoingPending(Boolean isOutgoingPending) {
        this.isOutgoingPending = isOutgoingPending;
    }

    public Boolean getIsIncomingPending() {
        return isIncomingPending;
    }

    public void setIsIncomingPending(Boolean isIncomingPending) {
        this.isIncomingPending = isIncomingPending;
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public TalentDto() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<Map<String, Object>> getCredits() {
        return credits;
    }

    public void setCredits(List<Map<String, Object>> credits) {
        this.credits = credits;
    }

    public String getShowreel() {
        return showreel;
    }

    public void setShowreel(String showreel) {
        this.showreel = showreel;
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

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
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

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
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

    public String getPrimaryCraft() {
        return primaryCraft != null ? primaryCraft : category;
    }

    public void setPrimaryCraft(String primaryCraft) {
        this.primaryCraft = primaryCraft;
    }

    public String getSubCraft() {
        return subCraft != null ? subCraft : role;
    }

    public void setSubCraft(String subCraft) {
        this.subCraft = subCraft;
    }
}

