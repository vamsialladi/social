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
    private String phone;
    private String email;
    private String experience;
    private String dateOfBirth;
    private String height;
    private String weight;
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

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getHeight() { return height; }
    public void setHeight(String height) { this.height = height; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

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

    // JSON Helper Getters and Setters
    public List<String> getLanguages() {
        return readList(languagesJson);
    }
    public void setLanguages(List<String> list) {
        this.languagesJson = writeJson(list);
    }

    public List<String> getTalents() {
        return readList(talentsJson);
    }
    public void setTalents(List<String> list) {
        this.talentsJson = writeJson(list);
    }

    public List<String> getWorkTypes() {
        return readList(workTypesJson);
    }
    public void setWorkTypes(List<String> list) {
        this.workTypesJson = writeJson(list);
    }

    public List<String> getVideoUrls() {
        return readList(videoUrlsJson);
    }
    public void setVideoUrls(List<String> list) {
        this.videoUrlsJson = writeJson(list);
    }

    public List<Map<String, Object>> getVideos() {
        return readObjectList(videosJson);
    }
    public void setVideos(List<Map<String, Object>> list) {
        this.videosJson = writeJson(list);
    }

    public List<Map<String, Object>> getExperiences() {
        return readObjectList(experiencesJson);
    }
    public void setExperiences(List<Map<String, Object>> list) {
        this.experiencesJson = writeJson(list);
    }

    // JSON serialization helpers
    private static List<String> readList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static List<Map<String, Object>> readObjectList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static String writeJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    public Map<String, Object> toDto() {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", id);
        dto.put("userId", userId);
        dto.put("name", name != null ? name : "");
        dto.put("phone", phone != null ? phone : "");
        dto.put("email", email != null ? email : "");
        dto.put("experience", experience != null ? experience : "");
        dto.put("dateOfBirth", dateOfBirth != null ? dateOfBirth : "");
        dto.put("height", height != null ? height : "");
        dto.put("weight", weight != null ? weight : "");
        dto.put("gender", gender != null ? gender : "");
        dto.put("skinTone", skinTone != null ? skinTone : "");
        dto.put("address", address != null ? address : "");
        dto.put("languages", getLanguages());
        dto.put("otherLanguage", otherLanguage != null ? otherLanguage : "");
        dto.put("talents", getTalents());
        dto.put("workTypes", getWorkTypes());
        dto.put("profileImageUrl", profileImageUrl != null ? profileImageUrl : "");
        dto.put("coverImageUrl", coverImageUrl != null ? coverImageUrl : "");
        dto.put("videos", getVideos());
        dto.put("videoUrls", getVideoUrls());
        dto.put("experiences", getExperiences());
        return dto;
    }
}
