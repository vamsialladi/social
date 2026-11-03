package com.truehunt.social.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfileUpdateRequest {
    private String userId;
    private String name;
    private String fullName;
    private String headline;
    private String role;
    private String category;
    private String company;
    private String companyName;
    private String headquarters;
    private String industry;
    private String location;
    private String address;
    private String bio;
    private String highlights;
    private String avatar;
    private String profileImageUrl;
    private String coverImage;
    private String coverImageUrl;
    private String phone;
    private String phoneNumber;
    private String email;
    private String dateOfBirth;
    private String experience;
    private String experienceYears;
    private String primaryCraft;
    private String craft;
    private String subCraft;
    private String height;
    private String weight;
    private String bodyBuild;
    private String gender;
    private String facialHair;
    private String skinTone;
    private String otherLanguage;
    private String websiteUrl;
    private String showreel;
    private String availability;
    private Boolean acceptedTerms;

    private List<String> skills;
    private List<String> talents;
    private List<String> languages;
    private List<String> secondaryCrafts;
    private List<String> secondarySubCrafts;
    private List<String> workTypes;
    private List<String> videoUrls;
    private List<Map<String, Object>> videos;
    private List<ExperienceDto> experiences;
    private List<EducationDto> educations;
    private List<ProjectDto> projects;

    public ProfileUpdateRequest() {}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return (name != null && !name.isBlank()) ? name : fullName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return (fullName != null && !fullName.isBlank()) ? fullName : name;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
        if (this.name == null || this.name.isBlank()) {
            this.name = fullName;
        }
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
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

    public String getCompany() {
        return (company != null && !company.isBlank()) ? company : companyName;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getCompanyName() {
        return (companyName != null && !companyName.isBlank()) ? companyName : company;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
        if (this.company == null || this.company.isBlank()) {
            this.company = companyName;
        }
    }

    public String getHeadquarters() {
        return headquarters;
    }

    public void setHeadquarters(String headquarters) {
        this.headquarters = headquarters;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getLocation() {
        return (location != null && !location.isBlank()) ? location : address;
    }

    public void setLocation(String location) {
        this.location = location;
        if (this.address == null || this.address.isBlank()) {
            this.address = location;
        }
    }

    public String getAddress() {
        return (address != null && !address.isBlank()) ? address : location;
    }

    public void setAddress(String address) {
        this.address = address;
        if (this.location == null || this.location.isBlank()) {
            this.location = address;
        }
    }

    public String getBio() {
        return (bio != null && !bio.isBlank()) ? bio : highlights;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getHighlights() {
        return (highlights != null && !highlights.isBlank()) ? highlights : bio;
    }

    public void setHighlights(String highlights) {
        this.highlights = highlights;
        if (this.bio == null || this.bio.isBlank()) {
            this.bio = highlights;
        }
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

    public String getPhone() {
        return (phone != null && !phone.isBlank()) ? phone : phoneNumber;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhoneNumber() {
        return (phoneNumber != null && !phoneNumber.isBlank()) ? phoneNumber : phone;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        if (this.phone == null || this.phone.isBlank()) {
            this.phone = phoneNumber;
        }
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getExperience() {
        return (experience != null && !experience.isBlank()) ? experience : experienceYears;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getExperienceYears() {
        return (experienceYears != null && !experienceYears.isBlank()) ? experienceYears : experience;
    }

    public void setExperienceYears(String experienceYears) {
        this.experienceYears = experienceYears;
        if (this.experience == null || this.experience.isBlank()) {
            this.experience = experienceYears;
        }
    }

    public String getPrimaryCraft() {
        return (primaryCraft != null && !primaryCraft.isBlank()) ? primaryCraft : craft;
    }

    public void setPrimaryCraft(String primaryCraft) {
        this.primaryCraft = primaryCraft;
    }

    public String getCraft() {
        return (craft != null && !craft.isBlank()) ? craft : primaryCraft;
    }

    public void setCraft(String craft) {
        this.craft = craft;
        if (this.primaryCraft == null || this.primaryCraft.isBlank()) {
            this.primaryCraft = craft;
        }
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

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public Boolean getAcceptedTerms() {
        return acceptedTerms;
    }

    public void setAcceptedTerms(Boolean acceptedTerms) {
        this.acceptedTerms = acceptedTerms;
    }

    public List<String> getSkills() {
        return (skills != null && !skills.isEmpty()) ? skills : talents;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<String> getTalents() {
        return (talents != null && !talents.isEmpty()) ? talents : skills;
    }

    public void setTalents(List<String> talents) {
        this.talents = talents;
        if (this.skills == null || this.skills.isEmpty()) {
            this.skills = talents;
        }
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
}
