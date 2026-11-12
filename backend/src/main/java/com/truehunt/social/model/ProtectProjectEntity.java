package com.truehunt.social.model;

import jakarta.persistence.*;

@Entity
@Table(name = "protect_projects")
public class ProtectProjectEntity {

    @Id
    private String id;

    private String category;
    private String title;
    private String productionCompany;
    private String producerName;
    private String producerEmail;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String synopsis;

    private String status;
    private String currentStage;
    private Integer stageProgress = 10;
    private String totalBudget;
    private String spentBudget;
    private String startDate;
    private String targetReleaseDate;
    private Integer seasonNumber;
    private Integer totalEpisodes;
    private String targetFestival;
    private String targetRuntime;

    @Column(length = 2048)
    private String bannerImageUrl;

    private String createdAt;
    private String updatedAt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String tasksJson = "[]";

    @Lob
    @Column(columnDefinition = "TEXT")
    private String documentsJson = "[]";

    @Lob
    @Column(columnDefinition = "TEXT")
    private String milestonesJson = "[]";

    @Lob
    @Column(columnDefinition = "TEXT")
    private String risksJson = "[]";

    @Lob
    @Column(columnDefinition = "TEXT")
    private String episodesJson = "[]";

    @Lob
    @Column(columnDefinition = "TEXT")
    private String recentActivityJson = "[]";

    public ProtectProjectEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getProductionCompany() { return productionCompany; }
    public void setProductionCompany(String productionCompany) { this.productionCompany = productionCompany; }

    public String getProducerName() { return producerName; }
    public void setProducerName(String producerName) { this.producerName = producerName; }

    public String getProducerEmail() { return producerEmail; }
    public void setProducerEmail(String producerEmail) { this.producerEmail = producerEmail; }

    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }

    public Integer getStageProgress() { return stageProgress; }
    public void setStageProgress(Integer stageProgress) { this.stageProgress = stageProgress; }

    public String getTotalBudget() { return totalBudget; }
    public void setTotalBudget(String totalBudget) { this.totalBudget = totalBudget; }

    public String getSpentBudget() { return spentBudget; }
    public void setSpentBudget(String spentBudget) { this.spentBudget = spentBudget; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getTargetReleaseDate() { return targetReleaseDate; }
    public void setTargetReleaseDate(String targetReleaseDate) { this.targetReleaseDate = targetReleaseDate; }

    public Integer getSeasonNumber() { return seasonNumber; }
    public void setSeasonNumber(Integer seasonNumber) { this.seasonNumber = seasonNumber; }

    public Integer getTotalEpisodes() { return totalEpisodes; }
    public void setTotalEpisodes(Integer totalEpisodes) { this.totalEpisodes = totalEpisodes; }

    public String getTargetFestival() { return targetFestival; }
    public void setTargetFestival(String targetFestival) { this.targetFestival = targetFestival; }

    public String getTargetRuntime() { return targetRuntime; }
    public void setTargetRuntime(String targetRuntime) { this.targetRuntime = targetRuntime; }

    public String getBannerImageUrl() { return bannerImageUrl; }
    public void setBannerImageUrl(String bannerImageUrl) { this.bannerImageUrl = bannerImageUrl; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getTasksJson() { return tasksJson; }
    public void setTasksJson(String tasksJson) { this.tasksJson = tasksJson; }

    public String getDocumentsJson() { return documentsJson; }
    public void setDocumentsJson(String documentsJson) { this.documentsJson = documentsJson; }

    public String getMilestonesJson() { return milestonesJson; }
    public void setMilestonesJson(String milestonesJson) { this.milestonesJson = milestonesJson; }

    public String getRisksJson() { return risksJson; }
    public void setRisksJson(String risksJson) { this.risksJson = risksJson; }

    public String getEpisodesJson() { return episodesJson; }
    public void setEpisodesJson(String episodesJson) { this.episodesJson = episodesJson; }

    public String getRecentActivityJson() { return recentActivityJson; }
    public void setRecentActivityJson(String recentActivityJson) { this.recentActivityJson = recentActivityJson; }
}

