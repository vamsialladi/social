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
    private String role;
    private String category;
    private String company;
    private String industry;
    private String websiteUrl;
    private String availability;
    private String phone;
    private String professionalPhone;
    private String email;
    private String experience;
    private String primaryCraft;
    private String subCraft;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String highlights;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String bio;

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
    private String facialHair;
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

    @Lob
    @Column(name = "profile_image_url", columnDefinition = "CLOB")
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

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProfessionalPhone() { return professionalPhone; }
    public void setProfessionalPhone(String professionalPhone) { this.professionalPhone = professionalPhone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getPrimaryCraft() { return primaryCraft; }
    public void setPrimaryCraft(String primaryCraft) { this.primaryCraft = primaryCraft; }

    public String getSubCraft() { return subCraft; }
    public void setSubCraft(String subCraft) { this.subCraft = subCraft; }

    public String getHighlights() { return (highlights != null && !highlights.isBlank()) ? highlights : (bio != null ? bio : ""); }
    public void setHighlights(String highlights) {
        this.highlights = highlights;
        if (this.bio == null || this.bio.isBlank()) {
            this.bio = highlights;
        }
    }

    public String getBio() { return (bio != null && !bio.isBlank()) ? bio : (highlights != null ? highlights : ""); }
    public void setBio(String bio) {
        this.bio = bio;
        if (this.highlights == null || this.highlights.isBlank()) {
            this.highlights = bio;
        }
    }

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

    public String getFacialHair() { return facialHair; }
    public void setFacialHair(String facialHair) { this.facialHair = facialHair; }

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
        String uid = (userId != null && !userId.isBlank()) ? userId : (id != null ? String.valueOf(id) : "me");
        dto.put("id", uid);
        dto.put("userId", uid);
        dto.put("referId", uid);
        dto.put("name", name != null ? name : "");
        dto.put("fullName", name != null ? name : "");

        String resolvedRole = (role != null && !role.isBlank()) ? role
                : ((subCraft != null && !subCraft.isBlank()) ? subCraft
                : ((primaryCraft != null && !primaryCraft.isBlank()) ? primaryCraft : "Cinema Professional"));
        dto.put("role", resolvedRole);

        String resolvedCategory = (category != null && !category.isBlank()) ? category
                : ((primaryCraft != null && !primaryCraft.isBlank()) ? primaryCraft : "Film Industry");
        dto.put("category", resolvedCategory);
        dto.put("craft", primaryCraft != null ? primaryCraft : "");
        dto.put("primaryCraft", primaryCraft != null ? primaryCraft : "");
        dto.put("subCraft", subCraft != null ? subCraft : "");

        dto.put("company", company != null ? company : "");
        dto.put("industry", industry != null ? industry : "");
        dto.put("websiteUrl", websiteUrl != null ? websiteUrl : "");
        dto.put("availability", (availability != null && !availability.isBlank()) ? availability : "Available for New Projects");
        dto.put("phone", phone != null ? phone : "");
        dto.put("phoneNumber", phone != null ? phone : "");
        dto.put("professionalPhone", professionalPhone != null ? professionalPhone : "");
        dto.put("email", email != null ? email : "");
        dto.put("experience", experience != null ? experience : "");
        dto.put("experienceYears", experience != null ? experience : "");

        dto.put("secondaryCrafts", getSecondaryCrafts());
        dto.put("secondarySubCrafts", getSecondarySubCrafts());
        dto.put("dateOfBirth", dateOfBirth != null ? dateOfBirth : "");
        dto.put("height", height != null ? height : "");
        dto.put("weight", weight != null ? weight : "");
        dto.put("bodyBuild", bodyBuild != null ? bodyBuild : "");
        dto.put("gender", gender != null ? gender : "");
        dto.put("facialHair", facialHair != null ? facialHair : "");
        dto.put("skinTone", skinTone != null ? skinTone : "");
        dto.put("address", address != null ? address : "");
        dto.put("location", (address != null && !address.isBlank()) ? address : "Hyderabad, India");

        String resolvedBio = (bio != null && !bio.isBlank()) ? bio : (highlights != null ? highlights : "");
        dto.put("bio", resolvedBio);
        dto.put("highlights", resolvedBio);

        dto.put("languages", getLanguages());
        dto.put("otherLanguage", otherLanguage != null ? otherLanguage : "");
        dto.put("talents", getTalents());
        dto.put("workTypes", getWorkTypes());

        List<String> allSkills = new ArrayList<>(getTalents());
        if (allSkills.isEmpty() && primaryCraft != null && !primaryCraft.isBlank()) {
            allSkills.add(primaryCraft);
        }
        dto.put("skills", allSkills);

        dto.put("profileImageUrl", profileImageUrl != null ? profileImageUrl : "");
        dto.put("avatar", profileImageUrl != null ? profileImageUrl : "");
        dto.put("coverImageUrl", coverImageUrl != null ? coverImageUrl : "");
        dto.put("coverImage", coverImageUrl != null ? coverImageUrl : "");

        dto.put("videoUrls", getVideoUrls());
        dto.put("videos", getVideos());
        dto.put("experiences", getExperiences());
        dto.put("educations", getEducations());
        dto.put("projects", getProjects());

        String resolvedHeadline = (headline != null && !headline.isBlank()) ? headline
                : (primaryCraft != null ? primaryCraft + " | Cinema Professional" : "Cinema Professional");
        dto.put("headline", resolvedHeadline);

        dto.put("verified", true);
        dto.put("connectionsCount", 128);
        dto.put("followersCount", 430);
        dto.put("isOwner", true);
        dto.put("isConnected", false);
        dto.put("isFollowing", false);
        return dto;
    }
}
