package com.truehunt.social.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperienceDto {
    private String id;
    private String title;
    private String company;
    private String employmentType;
    private String location;
    private String startDate;
    private String endDate;
    private Boolean isCurrent = false;
    private String description;
    private String highlights;
    private String companyLogoUrl;
    private String organizationLogoUrl;
    private Boolean featuredOnProfile = false;
    private Map<String, Object> attachment;

    // Alternative field aliases for social backward compatibility
    private String projectName;
    private String movieName;
    private String organizationName;
    private String workedAs;
    private String duration;

    public ExperienceDto() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        if (title != null && !title.isBlank()) return title;
        if (workedAs != null && !workedAs.isBlank()) return workedAs;
        if (projectName != null && !projectName.isBlank()) return projectName;
        return movieName;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        if (company != null && !company.isBlank()) return company;
        return organizationName;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Boolean getIsCurrent() {
        return isCurrent != null ? isCurrent : false;
    }

    public void setIsCurrent(Boolean current) {
        isCurrent = current;
    }

    public String getDescription() {
        if (description != null && !description.isBlank()) return description;
        return highlights;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHighlights() {
        if (highlights != null && !highlights.isBlank()) return highlights;
        return description;
    }

    public void setHighlights(String highlights) {
        this.highlights = highlights;
    }

    public String getCompanyLogoUrl() {
        if (companyLogoUrl != null && !companyLogoUrl.isBlank()) return companyLogoUrl;
        return organizationLogoUrl;
    }

    public void setCompanyLogoUrl(String companyLogoUrl) {
        this.companyLogoUrl = companyLogoUrl;
        if (this.organizationLogoUrl == null || this.organizationLogoUrl.isBlank()) {
            this.organizationLogoUrl = companyLogoUrl;
        }
    }

    public String getOrganizationLogoUrl() {
        if (organizationLogoUrl != null && !organizationLogoUrl.isBlank()) return organizationLogoUrl;
        return companyLogoUrl;
    }

    public void setOrganizationLogoUrl(String organizationLogoUrl) {
        this.organizationLogoUrl = organizationLogoUrl;
        if (this.companyLogoUrl == null || this.companyLogoUrl.isBlank()) {
            this.companyLogoUrl = organizationLogoUrl;
        }
    }

    public Boolean getFeaturedOnProfile() {
        return featuredOnProfile != null ? featuredOnProfile : false;
    }

    public void setFeaturedOnProfile(Boolean featuredOnProfile) {
        this.featuredOnProfile = featuredOnProfile;
    }

    public Map<String, Object> getAttachment() {
        return attachment;
    }

    public void setAttachment(Map<String, Object> attachment) {
        this.attachment = attachment;
    }

    public String getProjectName() {
        return (projectName != null && !projectName.isBlank()) ? projectName : getTitle();
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public String getOrganizationName() {
        return (organizationName != null && !organizationName.isBlank()) ? organizationName : getCompany();
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getWorkedAs() {
        return (workedAs != null && !workedAs.isBlank()) ? workedAs : getTitle();
    }

    public void setWorkedAs(String workedAs) {
        this.workedAs = workedAs;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
