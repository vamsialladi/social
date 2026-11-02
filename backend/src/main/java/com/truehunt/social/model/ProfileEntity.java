package com.truehunt.social.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "profiles")
public class ProfileEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String userId;

    private String name;
    private String headline;
    private String phone;
    private String email;
    private String experience;
    private String primaryCraft;
    private String subCraft;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String highlights;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String secondaryCraftsJson = "[]";
    @Lob
    @Column(columnDefinition = "TEXT")
    private String secondarySubCraftsJson = "[]";
    private String dateOfBirth;
    private String height;
    private String weight;
    private String bodyBuild;
    private String gender;
    private String skinTone;
    private String address;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String languagesJson = "[]";

    private String otherLanguage;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String talentsJson = "[]";

    @Lob
    @Column(columnDefinition = "TEXT")
    private String workTypesJson = "[]";

    @Column(length = 2048)
    private String profileImageUrl;

    @Column(length = 2048)
    private String coverImageUrl;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String videoUrlsJson = "[]";

    @Lob
    @Column(columnDefinition = "TEXT")
    private String videosJson = "[]";

    @Lob
    @Column(columnDefinition = "TEXT")
    private String experiencesJson = "[]";

    @Lob
    @Column(columnDefinition = "TEXT")
    private String educationsJson = "[]";

    @Lob
    @Column(columnDefinition = "TEXT")
    private String projectsJson = "[]";

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public ProfileEntity() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getPrimaryCraft() { return primaryCraft; }
    public void setPrimaryCraft(String primaryCraft) { this.primaryCraft = primaryCraft; }

    public String getSubCraft() { return subCraft; }
    public void setSubCraft(String subCraft) { this.subCraft = subCraft; }

    public String getHighlights() { return highlights != null ? highlights : ""; }
    public void setHighlights(String highlights) { this.highlights = highlights; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getHeight() { return height; }
    public void setHeight(String height) { this.height = height; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getBodyBuild() { return bodyBuild; }
    public void setBodyBuild(String bodyBuild) { this.bodyBuild = bodyBuild; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getSkinTone() { return skinTone; }
    public void setSkinTone(String skinTone) { this.skinTone = skinTone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getOtherLanguage() { return otherLanguage; }
    public void setOtherLanguage(String otherLanguage) { this.otherLanguage = otherLanguage; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // JSON mapped fields
    public List<String> getSecondaryCrafts() {
        return fromJson(secondaryCraftsJson, new TypeReference<List<String>>() {});
    }

    public void setSecondaryCrafts(List<String> secondaryCrafts) {
        this.secondaryCraftsJson = toJson(secondaryCrafts);
    }

    public List<String> getSecondarySubCrafts() {
        return fromJson(secondarySubCraftsJson, new TypeReference<List<String>>() {});
    }

    public void setSecondarySubCrafts(List<String> secondarySubCrafts) {
        this.secondarySubCraftsJson = toJson(secondarySubCrafts);
    }

    public List<String> getLanguages() {
        return fromJson(languagesJson, new TypeReference<List<String>>() {});
    }

    public void setLanguages(List<String> languages) {
        this.languagesJson = toJson(languages);
    }

    public List<String> getTalents() {
        return fromJson(talentsJson, new TypeReference<List<String>>() {});
    }

    public void setTalents(List<String> talents) {
        this.talentsJson = toJson(talents);
    }

    public List<String> getWorkTypes() {
        return fromJson(workTypesJson, new TypeReference<List<String>>() {});
    }

    public void setWorkTypes(List<String> workTypes) {
        this.workTypesJson = toJson(workTypes);
    }

    public List<String> getVideoUrls() {
        return fromJson(videoUrlsJson, new TypeReference<List<String>>() {});
    }

    public void setVideoUrls(List<String> videoUrls) {
        this.videoUrlsJson = toJson(videoUrls);
    }

    public List<Map<String, Object>> getVideos() {
        return fromJson(videosJson, new TypeReference<List<Map<String, Object>>>() {});
    }

    public void setVideos(List<Map<String, Object>> videos) {
        this.videosJson = toJson(videos);
    }

    public List<Map<String, Object>> getExperiences() {
        return fromJson(experiencesJson, new TypeReference<List<Map<String, Object>>>() {});
    }

    public void setExperiences(List<Map<String, Object>> experiences) {
        this.experiencesJson = toJson(experiences);
    }

    public List<Map<String, Object>> getEducations() {
        return fromJson(educationsJson, new TypeReference<List<Map<String, Object>>>() {});
    }

    public void setEducations(List<Map<String, Object>> educations) {
        this.educationsJson = toJson(educations);
    }

    public List<Map<String, Object>> getProjects() {
        return fromJson(projectsJson, new TypeReference<List<Map<String, Object>>>() {});
    }

    public void setProjects(List<Map<String, Object>> projects) {
        this.projectsJson = toJson(projects);
    }

    private <T> T fromJson(String json, TypeReference<T> ref) {
        if (json == null || json.isBlank()) return (T) Collections.emptyList();
        try {
            return MAPPER.readValue(json, ref);
        } catch (Exception e) {
            return (T) Collections.emptyList();
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    public Map<String, Object> toDto() {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", userId);
        dto.put("userId", userId);
        dto.put("name", name);
        dto.put("phone", phone);
        dto.put("email", email);
        dto.put("experience", experience);
        dto.put("primaryCraft", primaryCraft);
        dto.put("subCraft", subCraft);
        dto.put("secondaryCrafts", getSecondaryCrafts());
        dto.put("secondarySubCrafts", getSecondarySubCrafts());
        dto.put("dateOfBirth", dateOfBirth);
        dto.put("height", height);
        dto.put("weight", weight);
        dto.put("bodyBuild", bodyBuild);
        dto.put("gender", gender);
        dto.put("skinTone", skinTone);
        dto.put("address", address);
        dto.put("languages", getLanguages());
        dto.put("otherLanguage", otherLanguage);
        dto.put("talents", getTalents());
        dto.put("workTypes", getWorkTypes());
        dto.put("profileImageUrl", profileImageUrl);
        dto.put("coverImageUrl", coverImageUrl);
        dto.put("videoUrls", getVideoUrls());
        dto.put("videos", getVideos());
        dto.put("experiences", getExperiences());
        dto.put("educations", getEducations());
        dto.put("projects", getProjects());
        dto.put("skills", getTalents());
        dto.put("role", primaryCraft != null ? primaryCraft : "Cinema Professional");
        dto.put("category", primaryCraft != null ? primaryCraft : "Film Industry");
        dto.put("headline", headline != null && !headline.isBlank() ? headline :
                (primaryCraft != null ? primaryCraft + " | Cinema Professional" : "Cinema Professional"));
        dto.put("location", address != null ? address : "Hyderabad, India");
        dto.put("bio", highlights != null && !highlights.isBlank() ? highlights : "Cinema professional on TrueHunt.");
        dto.put("availability", "Available for New Projects");
        dto.put("verified", true);
        dto.put("connectionsCount", 128);
        dto.put("followersCount", 430);
        return dto;
    }
}
