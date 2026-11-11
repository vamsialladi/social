package com.truehunt.social.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProtectProjectDto {
    private String id;
    private String category;
    private String title;
    private String productionCompany;
    private String producerName;
    private String producerEmail;
    private String synopsis;
    private String status; // DEVELOPMENT, PRE_PRODUCTION, PRODUCTION, POST_PRODUCTION, RELEASE_MANAGEMENT, COMPLETED
    private String currentStage;
    private Integer stageProgress = 0;
    private String totalBudget;
    private String spentBudget;
    private String startDate;
    private String targetReleaseDate;
    private String bannerImageUrl;
    private Integer seasonNumber;
    private Integer totalEpisodes;
    private String targetFestival;
    private String targetRuntime;
    private String createdAt;
    private String updatedAt;

    private List<TaskDto> tasks = new ArrayList<>();
    private List<DocumentDto> documents = new ArrayList<>();
    private List<MilestoneDto> milestones = new ArrayList<>();
    private List<RiskDto> risks = new ArrayList<>();
    private List<EpisodeDto> episodes = new ArrayList<>();
    private List<Map<String, Object>> recentActivity = new ArrayList<>();

    public ProtectProjectDto() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProductionCompany() {
        return productionCompany;
    }

    public void setProductionCompany(String productionCompany) {
        this.productionCompany = productionCompany;
    }

    public String getProducerName() {
        return producerName;
    }

    public void setProducerName(String producerName) {
        this.producerName = producerName;
    }

    public String getProducerEmail() {
        return producerEmail;
    }

    public void setProducerEmail(String producerEmail) {
        this.producerEmail = producerEmail;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    public Integer getStageProgress() {
        return stageProgress;
    }

    public void setStageProgress(Integer stageProgress) {
        this.stageProgress = stageProgress;
    }

    public String getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(String totalBudget) {
        this.totalBudget = totalBudget;
    }

    public String getSpentBudget() {
        return spentBudget;
    }

    public void setSpentBudget(String spentBudget) {
        this.spentBudget = spentBudget;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getTargetReleaseDate() {
        return targetReleaseDate;
    }

    public void setTargetReleaseDate(String targetReleaseDate) {
        this.targetReleaseDate = targetReleaseDate;
    }

    public String getBannerImageUrl() {
        return bannerImageUrl;
    }

    public void setBannerImageUrl(String bannerImageUrl) {
        this.bannerImageUrl = bannerImageUrl;
    }

    public Integer getSeasonNumber() {
        return seasonNumber;
    }

    public void setSeasonNumber(Integer seasonNumber) {
        this.seasonNumber = seasonNumber;
    }

    public Integer getTotalEpisodes() {
        return totalEpisodes;
    }

    public void setTotalEpisodes(Integer totalEpisodes) {
        this.totalEpisodes = totalEpisodes;
    }

    public String getTargetFestival() {
        return targetFestival;
    }

    public void setTargetFestival(String targetFestival) {
        this.targetFestival = targetFestival;
    }

    public String getTargetRuntime() {
        return targetRuntime;
    }

    public void setTargetRuntime(String targetRuntime) {
        this.targetRuntime = targetRuntime;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<TaskDto> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDto> tasks) {
        this.tasks = tasks;
    }

    public List<DocumentDto> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentDto> documents) {
        this.documents = documents;
    }

    public List<MilestoneDto> getMilestones() {
        return milestones;
    }

    public void setMilestones(List<MilestoneDto> milestones) {
        this.milestones = milestones;
    }

    public List<RiskDto> getRisks() {
        return risks;
    }

    public void setRisks(List<RiskDto> risks) {
        this.risks = risks;
    }

    public List<EpisodeDto> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(List<EpisodeDto> episodes) {
        this.episodes = episodes;
    }

    public List<Map<String, Object>> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<Map<String, Object>> recentActivity) {
        this.recentActivity = recentActivity;
    }
}

