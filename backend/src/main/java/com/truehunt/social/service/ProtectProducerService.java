package com.truehunt.social.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truehunt.social.dto.*;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.model.ProtectProjectEntity;
import com.truehunt.social.repository.ProtectProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ProtectProducerService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Map<String, ProtectProjectDto> projectsMap = new ConcurrentHashMap<>();
    private final ProtectProjectRepository projectRepository;

    public ProtectProducerService() {
        this(null);
    }

    @Autowired
    public ProtectProducerService(ProtectProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
        initDemoProjects();
    }

    public List<Map<String, Object>> getCategories() {
        return List.of(
                buildCategory("movies", "Movies", "Movies / Feature Films", "MOVIE",
                        "Protect Your Vision. Manage Your Film.",
                        "Feature films, theatrical releases, and indie cinema. Master line budgeting, continuous shooting schedules, final cut approvals, and theatrical delivery readiness.",
                        "/assets/protect-producer/category_movies.webp",
                        "Film details, principal cast contracts, continuous shooting schedules, line budget vs actuals, director final cut, and CBFC censor certificate.",
                        "Theatrical & Festival Features", "14 Active Projects Tracked", "Film"),
                buildCategory("serials", "Serials", "Serials & Daily Soaps", "SERIAL",
                        "Every Episode Organized. Every Milestone Tracked.",
                        "Daily television drama and multi-episode broadcasts. Organize recurring shooting blocks, character arcs, episode-wise progress, and broadcast tape delivery.",
                        "/assets/protect-producer/category_serials.webp",
                        "Series information, episode list breakdown, episode-wise shooting progress, recurring unit schedules, and broadcast channel delivery.",
                        "Television & Daily Broadcast", "8 Active Broadcast Slates", "Tv"),
                buildCategory("web-series", "Web Series", "Web Series & Mini-Series", "WEB_SERIES",
                        "From Season Planning to Streaming Release.",
                        "Multi-season episodic storytelling for digital streamers. Supervise season roadmaps, OTT technical delivery specs, batch color grading, and release schedules.",
                        "/assets/protect-producer/category_web_series.webp",
                        "Season management, episode tracking, streaming platform technical requirements, multi-language dubbing versions, and OTT launch milestones.",
                        "OTT & Streaming Originals", "19 Active Streaming Seasons", "PlayCircle"),
                buildCategory("short-films", "Short Films", "Short Films & Festival Shorts", "SHORT_FILM",
                        "Small Productions. Professional Protection.",
                        "Compact festival contenders, narrative shorts, and indie passion projects. Streamline lean pre-production, essential rights clearance, and festival circuit tracking.",
                        "/assets/protect-producer/category_short_films.webp",
                        "Compact project setup, essential production tasks, rapid editing iterations, master festival deliverables, and digital premiere tracking.",
                        "Indie & Festival Showcase", "31 Active Showcase Projects", "Video")
        );
    }

    private Map<String, Object> buildCategory(
            String id, String title, String name, String categoryCode,
            String tagline, String description, String image,
            String workflowFocus, String badge, String stats, String icon) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("name", name);
        map.put("categoryCode", categoryCode);
        map.put("tagline", tagline);
        map.put("description", description);
        map.put("image", image);
        map.put("workflowFocus", workflowFocus);
        map.put("badge", badge);
        map.put("stats", stats);
        map.put("icon", icon);
        return map;
    }

    public List<ProtectProjectDto> getProjects(String category) {
        String normalizedFilter = normalizeCategory(category);
        return getAllProjectsList().stream()
                .filter(p -> normalizedFilter.isBlank() || normalizedFilter.equalsIgnoreCase("ALL")
                        || normalizeCategory(p.getCategory()).equalsIgnoreCase(normalizedFilter))
                .collect(Collectors.toList());
    }

    public ProtectProjectDto getProjectById(String id) {
        if (projectRepository != null) {
            Optional<ProtectProjectEntity> entity = projectRepository.findById(id);
            if (entity.isPresent()) {
                return toDto(entity.get());
            }
        }
        ProtectProjectDto project = projectsMap.get(id);
        if (project == null) {
            throw new ResourceNotFoundException("Project", "id", id);
        }
        return project;
    }

    public ProtectProjectDto createProject(ProtectProjectDto project) {
        if (project.getId() == null || project.getId().isBlank()) {
            project.setId("proj-" + UUID.randomUUID().toString().substring(0, 8));
        }
        project.setCategory(normalizeCategory(project.getCategory(), "MOVIE"));
        if (project.getStatus() == null || project.getStatus().isBlank()) project.setStatus("PRE_PRODUCTION");
        if (project.getCurrentStage() == null || project.getCurrentStage().isBlank()) project.setCurrentStage("PRE_PRODUCTION");
        if (project.getStageProgress() == null) project.setStageProgress(15);
        if (project.getCreatedAt() == null) project.setCreatedAt(LocalDateTime.now().toString());
        project.setUpdatedAt(LocalDateTime.now().toString());

        ensureListsInitialized(project);
        populateDefaultsIfEmpty(project);

        Map<String, Object> activity = new HashMap<>();
        activity.put("id", "act-" + UUID.randomUUID().toString().substring(0, 8));
        activity.put("projectId", project.getId());
        activity.put("action", "Project Registered");
        activity.put("performedBy", project.getProducerName() != null ? project.getProducerName() : "Producer");
        activity.put("details", "Project registered under TrueHunt Protect Producer platform.");
        activity.put("timestamp", LocalDateTime.now().toString());
        project.getRecentActivity().add(0, activity);

        return saveProject(project);
    }

    public ProtectProjectDto updateProject(String id, ProtectProjectDto updates) {
        ProtectProjectDto project = getProjectById(id);
        if (updates.getTitle() != null) project.setTitle(updates.getTitle());
        if (updates.getProductionCompany() != null) project.setProductionCompany(updates.getProductionCompany());
        if (updates.getProducerName() != null) project.setProducerName(updates.getProducerName());
        if (updates.getProducerEmail() != null) project.setProducerEmail(updates.getProducerEmail());
        if (updates.getSynopsis() != null) project.setSynopsis(updates.getSynopsis());
        if (updates.getTotalBudget() != null) project.setTotalBudget(updates.getTotalBudget());
        if (updates.getSpentBudget() != null) project.setSpentBudget(updates.getSpentBudget());
        if (updates.getStartDate() != null) project.setStartDate(updates.getStartDate());
        if (updates.getTargetReleaseDate() != null) project.setTargetReleaseDate(updates.getTargetReleaseDate());
        if (updates.getBannerImageUrl() != null) project.setBannerImageUrl(updates.getBannerImageUrl());
        if (updates.getTargetRuntime() != null) project.setTargetRuntime(updates.getTargetRuntime());
        if (updates.getTargetFestival() != null) project.setTargetFestival(updates.getTargetFestival());
        if (updates.getSeasonNumber() != null) project.setSeasonNumber(updates.getSeasonNumber());
        if (updates.getTotalEpisodes() != null) project.setTotalEpisodes(updates.getTotalEpisodes());
        if (updates.getStatus() != null) project.setStatus(updates.getStatus());
        project.setUpdatedAt(LocalDateTime.now().toString());
        return saveProject(project);
    }

    public void deleteProject(String id) {
        projectsMap.remove(id);
        if (projectRepository != null && projectRepository.existsById(id)) {
            projectRepository.deleteById(id);
        }
    }

    public ProtectProjectDto updateStage(String id, StageUpdateRequest req) {
        ProtectProjectDto project = getProjectById(id);
        ensureListsInitialized(project);
        if (req.getStage() != null) {
            project.setCurrentStage(req.getStage().toUpperCase().replace('-', '_'));
        }
        if (req.getProgress() != null) {
            project.setStageProgress(req.getProgress());
        }
        if (req.getStatus() != null) {
            project.setStatus(req.getStatus().toUpperCase().replace('-', '_'));
        }
        project.setUpdatedAt(LocalDateTime.now().toString());

        Map<String, Object> act = new HashMap<>();
        act.put("id", "act-" + UUID.randomUUID().toString().substring(0, 8));
        act.put("projectId", project.getId());
        act.put("action", "Stage Transition");
        act.put("performedBy", project.getProducerName() != null ? project.getProducerName() : "Producer");
        act.put("details", "Stage updated to " + project.getCurrentStage() + " (" + project.getStageProgress() + "% completed).");
        act.put("timestamp", LocalDateTime.now().toString());
        project.getRecentActivity().add(0, act);

        return saveProject(project);
    }

    // Tasks
    public List<TaskDto> getTasks(String projectId, String stage) {
        ProtectProjectDto project = getProjectById(projectId);
        String formatted = stage != null ? stage.toUpperCase().replace('-', '_') : "";
        return project.getTasks().stream()
                .filter(t -> formatted.isBlank() || (t.getStage() != null && t.getStage().equalsIgnoreCase(formatted)))
                .collect(Collectors.toList());
    }

    public TaskDto createTask(String projectId, TaskDto task) {
        ProtectProjectDto project = getProjectById(projectId);
        ensureListsInitialized(project);
        if (task.getId() == null || task.getId().isBlank()) {
            task.setId("tsk-" + UUID.randomUUID().toString().substring(0, 8));
        }
        task.setProjectId(projectId);
        if (task.getStage() == null) task.setStage(project.getCurrentStage());
        if (task.getStatus() == null) task.setStatus("NOT_STARTED");
        if (task.getPriority() == null) task.setPriority("MEDIUM");
        if (task.getApprovalStatus() == null) task.setApprovalStatus("NONE");

        project.getTasks().add(task);
        project.setUpdatedAt(LocalDateTime.now().toString());
        saveProject(project);
        return task;
    }

    public TaskDto updateTask(String projectId, String taskId, TaskDto updates) {
        ProtectProjectDto project = getProjectById(projectId);
        for (TaskDto t : project.getTasks()) {
            if (Objects.equals(t.getId(), taskId)) {
                if (updates.getTitle() != null) t.setTitle(updates.getTitle());
                if (updates.getDescription() != null) t.setDescription(updates.getDescription());
                if (updates.getAssignedTo() != null) t.setAssignedTo(updates.getAssignedTo());
                if (updates.getStatus() != null) t.setStatus(updates.getStatus());
                if (updates.getPriority() != null) t.setPriority(updates.getPriority());
                if (updates.getDueDate() != null) t.setDueDate(updates.getDueDate());
                if (updates.getApprovalStatus() != null) {
                    t.setApprovalStatus(updates.getApprovalStatus());
                    if ("APPROVED".equalsIgnoreCase(updates.getApprovalStatus())) {
                        t.setApprovedAt(LocalDateTime.now().toString());
                        t.setApprovedBy(updates.getApprovedBy() != null ? updates.getApprovedBy() : "Producer");
                    }
                }
                project.setUpdatedAt(LocalDateTime.now().toString());
                saveProject(project);
                return t;
            }
        }
        throw new ResourceNotFoundException("Task", "id", taskId);
    }

    // Documents
    public List<DocumentDto> getDocuments(String projectId, String stage) {
        ProtectProjectDto project = getProjectById(projectId);
        String formatted = stage != null ? stage.toUpperCase().replace('-', '_') : "";
        return project.getDocuments().stream()
                .filter(d -> formatted.isBlank() || (d.getStage() != null && d.getStage().equalsIgnoreCase(formatted)))
                .collect(Collectors.toList());
    }

    public DocumentDto createDocument(String projectId, DocumentDto doc) {
        ProtectProjectDto project = getProjectById(projectId);
        ensureListsInitialized(project);
        if (doc.getId() == null || doc.getId().isBlank()) {
            doc.setId("doc-" + UUID.randomUUID().toString().substring(0, 8));
        }
        doc.setProjectId(projectId);
        if (doc.getStage() == null) doc.setStage(project.getCurrentStage());
        if (doc.getCreatedAt() == null) doc.setCreatedAt(LocalDateTime.now().toString());
        project.getDocuments().add(0, doc);
        project.setUpdatedAt(LocalDateTime.now().toString());
        saveProject(project);
        return doc;
    }

    public DocumentDto verifyDocument(String projectId, String documentId, Boolean verified) {
        ProtectProjectDto project = getProjectById(projectId);
        for (DocumentDto d : project.getDocuments()) {
            if (Objects.equals(d.getId(), documentId)) {
                d.setVerified(verified != null && verified);
                project.setUpdatedAt(LocalDateTime.now().toString());
                saveProject(project);
                return d;
            }
        }
        throw new ResourceNotFoundException("Document", "id", documentId);
    }

    public DocumentDto updateDocument(String projectId, String documentId, DocumentDto updates) {
        ProtectProjectDto project = getProjectById(projectId);
        for (DocumentDto d : project.getDocuments()) {
            if (Objects.equals(d.getId(), documentId)) {
                if (updates.getTitle() != null) d.setTitle(updates.getTitle());
                if (updates.getDocumentType() != null) d.setDocumentType(updates.getDocumentType());
                if (updates.getFileUrl() != null) d.setFileUrl(updates.getFileUrl());
                if (updates.getNotes() != null) d.setNotes(updates.getNotes());
                if (updates.getVerified() != null) d.setVerified(updates.getVerified());
                if (updates.getStage() != null) d.setStage(updates.getStage());
                project.setUpdatedAt(LocalDateTime.now().toString());
                saveProject(project);
                return d;
            }
        }
        throw new ResourceNotFoundException("Document", "id", documentId);
    }

    // Milestones
    public List<MilestoneDto> getMilestones(String projectId, String stage) {
        ProtectProjectDto project = getProjectById(projectId);
        String formatted = stage != null ? stage.toUpperCase().replace('-', '_') : "";
        return project.getMilestones().stream()
                .filter(m -> formatted.isBlank() || (m.getStage() != null && m.getStage().equalsIgnoreCase(formatted)))
                .collect(Collectors.toList());
    }

    public MilestoneDto toggleMilestone(String projectId, String milestoneId) {
        ProtectProjectDto project = getProjectById(projectId);
        for (MilestoneDto m : project.getMilestones()) {
            if (Objects.equals(m.getId(), milestoneId)) {
                m.setIsCompleted(!Boolean.TRUE.equals(m.getIsCompleted()));
                m.setCompletedDate(Boolean.TRUE.equals(m.getIsCompleted()) ? LocalDateTime.now().toLocalDate().toString() : null);
                project.setUpdatedAt(LocalDateTime.now().toString());
                saveProject(project);
                return m;
            }
        }
        throw new ResourceNotFoundException("Milestone", "id", milestoneId);
    }

    // Risks
    public List<RiskDto> getRisks(String projectId, String stage) {
        ProtectProjectDto project = getProjectById(projectId);
        String formatted = stage != null ? stage.toUpperCase().replace('-', '_') : "";
        return project.getRisks().stream()
                .filter(r -> formatted.isBlank() || (r.getStage() != null && r.getStage().equalsIgnoreCase(formatted)))
                .collect(Collectors.toList());
    }

    public RiskDto createRisk(String projectId, RiskDto risk) {
        ProtectProjectDto project = getProjectById(projectId);
        ensureListsInitialized(project);
        if (risk.getId() == null || risk.getId().isBlank()) {
            risk.setId("rsk-" + UUID.randomUUID().toString().substring(0, 8));
        }
        risk.setProjectId(projectId);
        if (risk.getStage() == null) risk.setStage(project.getCurrentStage());
        if (risk.getCreatedAt() == null) risk.setCreatedAt(LocalDateTime.now().toString());
        project.getRisks().add(0, risk);
        project.setUpdatedAt(LocalDateTime.now().toString());
        saveProject(project);
        return risk;
    }

    public RiskDto updateRisk(String projectId, String riskId, RiskDto updates) {
        ProtectProjectDto project = getProjectById(projectId);
        for (RiskDto r : project.getRisks()) {
            if (Objects.equals(r.getId(), riskId)) {
                if (updates.getTitle() != null) r.setTitle(updates.getTitle());
                if (updates.getSeverity() != null) r.setSeverity(updates.getSeverity());
                if (updates.getMitigationPlan() != null) r.setMitigationPlan(updates.getMitigationPlan());
                if (updates.getOwner() != null) r.setOwner(updates.getOwner());
                if (updates.getStatus() != null) r.setStatus(updates.getStatus());
                project.setUpdatedAt(LocalDateTime.now().toString());
                saveProject(project);
                return r;
            }
        }
        throw new ResourceNotFoundException("Risk", "id", riskId);
    }

    // Episodes
    public List<EpisodeDto> getEpisodes(String projectId) {
        return getProjectById(projectId).getEpisodes();
    }

    public EpisodeDto createEpisode(String projectId, EpisodeDto episode) {
        ProtectProjectDto project = getProjectById(projectId);
        ensureListsInitialized(project);
        if (episode.getId() == null || episode.getId().isBlank()) {
            episode.setId("ep-" + UUID.randomUUID().toString().substring(0, 8));
        }
        episode.setProjectId(projectId);
        if (episode.getEpisodeNumber() == null) {
            episode.setEpisodeNumber(project.getEpisodes().size() + 1);
        }
        project.getEpisodes().add(episode);
        project.setUpdatedAt(LocalDateTime.now().toString());
        saveProject(project);
        return episode;
    }

    public EpisodeDto updateEpisode(String projectId, String epId, EpisodeDto updates) {
        ProtectProjectDto project = getProjectById(projectId);
        for (EpisodeDto e : project.getEpisodes()) {
            if (Objects.equals(e.getId(), epId)) {
                if (updates.getTitle() != null) e.setTitle(updates.getTitle());
                if (updates.getStatus() != null) e.setStatus(updates.getStatus());
                if (updates.getDuration() != null) e.setDuration(updates.getDuration());
                if (updates.getAirDate() != null) e.setAirDate(updates.getAirDate());
                project.setUpdatedAt(LocalDateTime.now().toString());
                saveProject(project);
                return e;
            }
        }
        throw new ResourceNotFoundException("Episode", "id", epId);
    }

    // Activity
    public List<Map<String, Object>> getActivityLogs(String projectId) {
        return getProjectById(projectId).getRecentActivity();
    }

    private String normalizeCategory(String category) {
        return normalizeCategory(category, "");
    }

    private String normalizeCategory(String category, String defaultValue) {
        if (category == null || category.isBlank()) {
            return defaultValue;
        }
        String norm = category.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (norm) {
            case "MOVIES", "MOVIE" -> "MOVIE";
            case "SERIALS", "SERIAL" -> "SERIAL";
            case "WEB_SERIES", "WEBSERIES" -> "WEB_SERIES";
            case "SHORT_FILMS", "SHORT_FILM", "SHORTFILMS" -> "SHORT_FILM";
            default -> norm;
        };
    }

    private void ensureListsInitialized(ProtectProjectDto project) {
        if (project.getTasks() == null) project.setTasks(new ArrayList<>());
        if (project.getDocuments() == null) project.setDocuments(new ArrayList<>());
        if (project.getMilestones() == null) project.setMilestones(new ArrayList<>());
        if (project.getRisks() == null) project.setRisks(new ArrayList<>());
        if (project.getEpisodes() == null) project.setEpisodes(new ArrayList<>());
        if (project.getRecentActivity() == null) project.setRecentActivity(new ArrayList<>());
    }

    private void populateDefaultsIfEmpty(ProtectProjectDto project) {
        String id = project.getId();
        if (project.getTasks().isEmpty()) {
            project.getTasks().add(createDefaultTask(id, "PRE_PRODUCTION", "Script Lock & Scene Breakdown",
                    "Complete scene-by-scene breakdown and technical schedule", "Writers Room", "IN_PROGRESS", "CRITICAL", "PENDING"));
            project.getTasks().add(createDefaultTask(id, "PRE_PRODUCTION", "Line Budget Approval & Sign-off",
                    "Lock department budgets with Producer approval", "Line Producer", "NOT_STARTED", "HIGH", "NONE"));
            project.getTasks().add(createDefaultTask(id, "PRE_PRODUCTION", "Key Crew Contracts & Location Clearances",
                    "Issue binding crew engagements and municipal permits", "Legal & Admin", "NOT_STARTED", "HIGH", "NONE"));
        }

        if (project.getDocuments().isEmpty()) {
            DocumentDto d1 = new DocumentDto();
            d1.setId("doc-" + UUID.randomUUID().toString().substring(0, 8));
            d1.setProjectId(id);
            d1.setStage("PRE_PRODUCTION");
            d1.setDocumentType("CONCEPT_NOTE");
            d1.setTitle("Initial Concept & Story Outline");
            d1.setFileUrl("#");
            d1.setUploadedBy(project.getProducerName() != null ? project.getProducerName() : "Lead Producer");
            d1.setVerified(false);
            d1.setNotes("Initial registration dossier submitted during workspace initialization.");
            d1.setCreatedAt(LocalDateTime.now().toString());
            project.getDocuments().add(d1);
        }

        if (project.getMilestones().isEmpty()) {
            project.getMilestones().add(createDefaultMilestone(id, "PRE_PRODUCTION", "Pre-Production Completed & Approved"));
            project.getMilestones().add(createDefaultMilestone(id, "PRODUCTION", "Principal Photography Wrapped"));
            project.getMilestones().add(createDefaultMilestone(id, "POST_PRODUCTION", "Post-Production Picture Lock"));
            project.getMilestones().add(createDefaultMilestone(id, "RELEASE_MANAGEMENT", "Release Readiness & Distribution Clear"));
        }

        if (project.getEpisodes().isEmpty()
                && ("SERIAL".equalsIgnoreCase(project.getCategory()) || "WEB_SERIES".equalsIgnoreCase(project.getCategory()))) {
            EpisodeDto ep1 = new EpisodeDto();
            ep1.setId("ep-" + UUID.randomUUID().toString().substring(0, 8));
            ep1.setProjectId(id);
            ep1.setEpisodeNumber(1);
            ep1.setTitle("Pilot Episode");
            ep1.setStatus("IN_PROGRESS");
            ep1.setDuration("WEB_SERIES".equalsIgnoreCase(project.getCategory()) ? "42 mins" : "22 mins");
            ep1.setAirDate(project.getStartDate() != null ? project.getStartDate() : "2027-01-15");
            project.getEpisodes().add(ep1);
        }
    }

    private TaskDto createDefaultTask(String projectId, String stage, String title, String description,
                                      String assignedTo, String status, String priority, String approvalStatus) {
        TaskDto t = new TaskDto();
        t.setId("tsk-" + UUID.randomUUID().toString().substring(0, 8));
        t.setProjectId(projectId);
        t.setStage(stage);
        t.setTitle(title);
        t.setDescription(description);
        t.setAssignedTo(assignedTo);
        t.setStatus(status);
        t.setPriority(priority);
        t.setDueDate("Day 10");
        t.setApprovalStatus(approvalStatus);
        return t;
    }

    private MilestoneDto createDefaultMilestone(String projectId, String stage, String title) {
        MilestoneDto m = new MilestoneDto();
        m.setId("mls-" + UUID.randomUUID().toString().substring(0, 8));
        m.setProjectId(projectId);
        m.setStage(stage);
        m.setTitle(title);
        m.setTargetDate("2027-03-01");
        m.setIsCompleted(false);
        return m;
    }

    private List<ProtectProjectDto> getAllProjectsList() {
        if (projectRepository != null) {
            return projectRepository.findAllByOrderByUpdatedAtDesc().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>(projectsMap.values());
    }

    private ProtectProjectDto saveProject(ProtectProjectDto project) {
        projectsMap.put(project.getId(), project);
        if (projectRepository != null) {
            ProtectProjectEntity entity = toEntity(project);
            return toDto(projectRepository.save(entity));
        }
        return project;
    }

    private ProtectProjectEntity toEntity(ProtectProjectDto dto) {
        ProtectProjectEntity entity = new ProtectProjectEntity();
        entity.setId(dto.getId());
        entity.setCategory(dto.getCategory());
        entity.setTitle(dto.getTitle());
        entity.setProductionCompany(dto.getProductionCompany());
        entity.setProducerName(dto.getProducerName());
        entity.setProducerEmail(dto.getProducerEmail());
        entity.setSynopsis(dto.getSynopsis());
        entity.setStatus(dto.getStatus());
        entity.setCurrentStage(dto.getCurrentStage());
        entity.setStageProgress(dto.getStageProgress());
        entity.setTotalBudget(dto.getTotalBudget());
        entity.setSpentBudget(dto.getSpentBudget());
        entity.setStartDate(dto.getStartDate());
        entity.setTargetReleaseDate(dto.getTargetReleaseDate());
        entity.setSeasonNumber(dto.getSeasonNumber());
        entity.setTotalEpisodes(dto.getTotalEpisodes());
        entity.setTargetFestival(dto.getTargetFestival());
        entity.setTargetRuntime(dto.getTargetRuntime());
        entity.setBannerImageUrl(dto.getBannerImageUrl());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        entity.setTasksJson(writeJson(dto.getTasks()));
        entity.setDocumentsJson(writeJson(dto.getDocuments()));
        entity.setMilestonesJson(writeJson(dto.getMilestones()));
        entity.setRisksJson(writeJson(dto.getRisks()));
        entity.setEpisodesJson(writeJson(dto.getEpisodes()));
        entity.setRecentActivityJson(writeJson(dto.getRecentActivity()));
        return entity;
    }

    private ProtectProjectDto toDto(ProtectProjectEntity entity) {
        ProtectProjectDto dto = new ProtectProjectDto();
        dto.setId(entity.getId());
        dto.setCategory(entity.getCategory());
        dto.setTitle(entity.getTitle());
        dto.setProductionCompany(entity.getProductionCompany());
        dto.setProducerName(entity.getProducerName());
        dto.setProducerEmail(entity.getProducerEmail());
        dto.setSynopsis(entity.getSynopsis());
        dto.setStatus(entity.getStatus());
        dto.setCurrentStage(entity.getCurrentStage());
        dto.setStageProgress(entity.getStageProgress());
        dto.setTotalBudget(entity.getTotalBudget());
        dto.setSpentBudget(entity.getSpentBudget());
        dto.setStartDate(entity.getStartDate());
        dto.setTargetReleaseDate(entity.getTargetReleaseDate());
        dto.setSeasonNumber(entity.getSeasonNumber());
        dto.setTotalEpisodes(entity.getTotalEpisodes());
        dto.setTargetFestival(entity.getTargetFestival());
        dto.setTargetRuntime(entity.getTargetRuntime());
        dto.setBannerImageUrl(entity.getBannerImageUrl());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setTasks(readJson(entity.getTasksJson(), new TypeReference<>() {}));
        dto.setDocuments(readJson(entity.getDocumentsJson(), new TypeReference<>() {}));
        dto.setMilestones(readJson(entity.getMilestonesJson(), new TypeReference<>() {}));
        dto.setRisks(readJson(entity.getRisksJson(), new TypeReference<>() {}));
        dto.setEpisodes(readJson(entity.getEpisodesJson(), new TypeReference<>() {}));
        dto.setRecentActivity(readJson(entity.getRecentActivityJson(), new TypeReference<>() {}));
        return dto;
    }

    private String writeJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value != null ? value : List.of());
        } catch (Exception e) {
            return "[]";
        }
    }

    private <T> List<T> readJson(String json, TypeReference<List<T>> typeRef) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void initDemoProjects() {
        if (projectRepository != null && projectRepository.count() > 0) {
            return;
        }

        // Project 1: Feature Film
        ProtectProjectDto p1 = new ProtectProjectDto();
        p1.setId("proj-movie-1");
        p1.setCategory("MOVIE");
        p1.setTitle("The Red Monsoon");
        p1.setProductionCompany("Aura Motion Pictures & Mythri Slate");
        p1.setProducerName("Vikramaditya Roy");
        p1.setProducerEmail("vikram@aurafilms.in");
        p1.setSynopsis("A high-stakes political investigative thriller set across the coastal ports of South India.");
        p1.setStatus("IN_PRODUCTION");
        p1.setCurrentStage("PRODUCTION");
        p1.setStageProgress(45);
        p1.setTotalBudget("₹24,00,00,000");
        p1.setSpentBudget("₹11,40,00,000");
        p1.setStartDate("2026-08-15");
        p1.setTargetReleaseDate("2027-04-14");
        p1.setBannerImageUrl("/assets/protect-producer/category_movies.webp");
        p1.setTargetRuntime("158 mins");
        p1.setCreatedAt("2026-07-10T10:00:00Z");
        p1.setUpdatedAt("2026-10-14T12:00:00Z");

        TaskDto t1 = createDefaultTask(p1.getId(), "PRE_PRODUCTION", "Principal Script Lock & Scene Breakdown",
                "Scene-by-scene script breakdown with locked draft v3.2", "Writers Room", "COMPLETED", "CRITICAL", "APPROVED");
        t1.setId("tsk-m-1");
        t1.setDueDate("2026-08-01");
        p1.getTasks().add(t1);

        DocumentDto d1 = new DocumentDto();
        d1.setId("doc-m-1");
        d1.setProjectId(p1.getId());
        d1.setStage("PRE_PRODUCTION");
        d1.setDocumentType("SCRIPT");
        d1.setTitle("Master Screenplay Draft v3.2 (Locked)");
        d1.setFileUrl("#");
        d1.setUploadedBy("Writer - Karan Mehta");
        d1.setVerified(true);
        d1.setNotes("Signed off by Director and Lead Producer");
        d1.setCreatedAt("2026-08-01T12:00:00Z");
        p1.getDocuments().add(d1);

        MilestoneDto m1 = createDefaultMilestone(p1.getId(), "PRE_PRODUCTION", "Script Locked & Budget Approved");
        m1.setId("mls-m-1");
        m1.setTargetDate("2026-07-25");
        m1.setCompletedDate("2026-07-25");
        m1.setIsCompleted(true);
        p1.getMilestones().add(m1);

        Map<String, Object> act1 = new HashMap<>();
        act1.put("id", "act-m-1");
        act1.put("projectId", p1.getId());
        act1.put("action", "Project Registered");
        act1.put("performedBy", "Vikramaditya Roy");
        act1.put("details", "Project registered under TrueHunt Protect Producer platform.");
        act1.put("timestamp", "2026-07-10T10:00:00Z");
        p1.getRecentActivity().add(act1);

        saveProject(p1);

        // Project 2: Serial
        ProtectProjectDto p2 = new ProtectProjectDto();
        p2.setId("proj-serial-1");
        p2.setCategory("SERIAL");
        p2.setTitle("Vamsam: The Dynasty");
        p2.setProductionCompany("Sun Studio Productions & Gemini Prime");
        p2.setProducerName("Radhika Sundaram");
        p2.setProducerEmail("radhika@sunstudio.in");
        p2.setSynopsis("A multi-generational family saga charting power struggles across three generations.");
        p2.setStatus("IN_PRODUCTION");
        p2.setCurrentStage("PRODUCTION");
        p2.setStageProgress(60);
        p2.setTotalBudget("₹4,80,00,000");
        p2.setSpentBudget("₹2,90,00,000");
        p2.setStartDate("2026-06-01");
        p2.setTargetReleaseDate("2027-01-15");
        p2.setBannerImageUrl("/assets/protect-producer/category_serials.webp");
        p2.setTotalEpisodes(120);
        p2.setCreatedAt("2026-05-15T09:00:00Z");
        p2.setUpdatedAt("2026-10-15T15:00:00Z");

        EpisodeDto ep1 = new EpisodeDto();
        ep1.setId("ep-s-1");
        ep1.setProjectId(p2.getId());
        ep1.setEpisodeNumber(1);
        ep1.setTitle("The Homecoming");
        ep1.setStatus("AIRED");
        ep1.setDuration("24 mins");
        ep1.setAirDate("2026-07-01");
        p2.getEpisodes().add(ep1);

        saveProject(p2);
    }
}
