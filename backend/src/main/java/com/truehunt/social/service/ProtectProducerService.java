package com.truehunt.social.service;

import com.truehunt.social.dto.*;
import com.truehunt.social.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ProtectProducerService {

    private final Map<String, ProtectProjectDto> projectsMap = new ConcurrentHashMap<>();

    public ProtectProducerService() {
        initDemoProjects();
    }

    public List<Map<String, Object>> getCategories() {
        return List.of(
                Map.of("id", "movies", "name", "Movies / Feature Films", "categoryCode", "MOVIE", "description", "Theatrical and direct-to-OTT feature film productions", "icon", "Film"),
                Map.of("id", "serials", "name", "Serials & Daily Soaps", "categoryCode", "SERIAL", "description", "Broadcast TV serials, daily productions and episodic soap formats", "icon", "Tv"),
                Map.of("id", "web-series", "name", "Web Series & Mini-Series", "categoryCode", "WEB_SERIES", "description", "Streaming originals, seasonal episodic and mini-series formats", "icon", "PlayCircle"),
                Map.of("id", "short-films", "name", "Short Films & Festival Shorts", "categoryCode", "SHORT_FILM", "description", "Narrative shorts, student projects and festival circuit films", "icon", "Video"),
                Map.of("id", "documentaries", "name", "Documentaries & Non-Fiction", "categoryCode", "DOCUMENTARY", "description", "Investigative, biographical, historical and nature docs", "icon", "BookOpen"),
                Map.of("id", "commercials", "name", "Commercials & Brand Films", "categoryCode", "COMMERCIAL", "description", "TVCs, digital brand campaigns, corporate films and product videos", "icon", "Megaphone"),
                Map.of("id", "music-videos", "name", "Music Videos & Visuals", "categoryCode", "MUSIC_VIDEO", "description", "Single release videos, album visuals and artist performance pieces", "icon", "Music")
        );
    }

    public List<ProtectProjectDto> getProjects(String category) {
        String formattedCat = (category != null && !category.isBlank())
                ? category.toUpperCase().replace('-', '_')
                : "";

        return projectsMap.values().stream()
                .filter(p -> formattedCat.isBlank() || formattedCat.equalsIgnoreCase("ALL")
                        || p.getCategory().equalsIgnoreCase(formattedCat))
                .collect(Collectors.toList());
    }

    public ProtectProjectDto getProjectById(String id) {
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
        if (project.getCategory() != null) {
            project.setCategory(project.getCategory().toUpperCase().replace('-', '_'));
        } else {
            project.setCategory("MOVIE");
        }
        if (project.getStatus() == null) project.setStatus("PRE_PRODUCTION");
        if (project.getCurrentStage() == null) project.setCurrentStage("PRE_PRODUCTION");
        if (project.getStageProgress() == null) project.setStageProgress(15);
        if (project.getCreatedAt() == null) project.setCreatedAt(LocalDateTime.now().toString());
        project.setUpdatedAt(LocalDateTime.now().toString());

        if (project.getTasks() == null) project.setTasks(new ArrayList<>());
        if (project.getDocuments() == null) project.setDocuments(new ArrayList<>());
        if (project.getMilestones() == null) project.setMilestones(new ArrayList<>());
        if (project.getRisks() == null) project.setRisks(new ArrayList<>());
        if (project.getEpisodes() == null) project.setEpisodes(new ArrayList<>());
        if (project.getRecentActivity() == null) project.setRecentActivity(new ArrayList<>());

        // Default tasks & milestones if empty
        if (project.getTasks().isEmpty()) {
            TaskDto t1 = new TaskDto();
            t1.setId("tsk-" + UUID.randomUUID().toString().substring(0, 8));
            t1.setProjectId(project.getId());
            t1.setStage("PRE_PRODUCTION");
            t1.setTitle("Script Lock & Breakdown");
            t1.setDescription("Complete scene breakdown");
            t1.setAssignedTo("Writers Room");
            t1.setStatus("IN_PROGRESS");
            t1.setPriority("CRITICAL");
            t1.setDueDate("Day 10");
            t1.setApprovalStatus("PENDING");
            project.getTasks().add(t1);
        }

        Map<String, Object> activity = new HashMap<>();
        activity.put("id", "act-" + UUID.randomUUID().toString().substring(0, 8));
        activity.put("action", "Project Registered");
        activity.put("performedBy", project.getProducerName() != null ? project.getProducerName() : "Producer");
        activity.put("details", "Project registered under TrueHunt Protect Producer platform.");
        activity.put("timestamp", LocalDateTime.now().toString());
        project.getRecentActivity().add(0, activity);

        projectsMap.put(project.getId(), project);
        return project;
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
        project.setUpdatedAt(LocalDateTime.now().toString());
        return project;
    }

    public void deleteProject(String id) {
        projectsMap.remove(id);
    }

    public ProtectProjectDto updateStage(String id, StageUpdateRequest req) {
        ProtectProjectDto project = getProjectById(id);
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
        act.put("action", "Stage Transition");
        act.put("performedBy", project.getProducerName() != null ? project.getProducerName() : "Producer");
        act.put("details", "Stage updated to " + project.getCurrentStage() + " (" + project.getStageProgress() + "% completed).");
        act.put("timestamp", LocalDateTime.now().toString());
        project.getRecentActivity().add(0, act);

        return project;
    }

    // Tasks
    public List<TaskDto> getTasks(String projectId, String stage) {
        ProtectProjectDto project = getProjectById(projectId);
        String formatted = stage != null ? stage.toUpperCase().replace('-', '_') : "";
        return project.getTasks().stream()
                .filter(t -> formatted.isBlank() || t.getStage().equalsIgnoreCase(formatted))
                .collect(Collectors.toList());
    }

    public TaskDto createTask(String projectId, TaskDto task) {
        ProtectProjectDto project = getProjectById(projectId);
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
        return task;
    }

    public TaskDto updateTask(String projectId, String taskId, TaskDto updates) {
        ProtectProjectDto project = getProjectById(projectId);
        for (int i = 0; i < project.getTasks().size(); i++) {
            TaskDto t = project.getTasks().get(i);
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
                .filter(d -> formatted.isBlank() || d.getStage().equalsIgnoreCase(formatted))
                .collect(Collectors.toList());
    }

    public DocumentDto createDocument(String projectId, DocumentDto doc) {
        ProtectProjectDto project = getProjectById(projectId);
        if (doc.getId() == null || doc.getId().isBlank()) {
            doc.setId("doc-" + UUID.randomUUID().toString().substring(0, 8));
        }
        doc.setProjectId(projectId);
        if (doc.getStage() == null) doc.setStage(project.getCurrentStage());
        if (doc.getCreatedAt() == null) doc.setCreatedAt(LocalDateTime.now().toString());
        project.getDocuments().add(0, doc);
        project.setUpdatedAt(LocalDateTime.now().toString());
        return doc;
    }

    public DocumentDto verifyDocument(String projectId, String documentId, Boolean verified) {
        ProtectProjectDto project = getProjectById(projectId);
        for (DocumentDto d : project.getDocuments()) {
            if (Objects.equals(d.getId(), documentId)) {
                d.setVerified(verified != null && verified);
                project.setUpdatedAt(LocalDateTime.now().toString());
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
                .filter(m -> formatted.isBlank() || m.getStage().equalsIgnoreCase(formatted))
                .collect(Collectors.toList());
    }

    public MilestoneDto toggleMilestone(String projectId, String milestoneId) {
        ProtectProjectDto project = getProjectById(projectId);
        for (MilestoneDto m : project.getMilestones()) {
            if (Objects.equals(m.getId(), milestoneId)) {
                m.setIsCompleted(!m.getIsCompleted());
                m.setCompletedDate(m.getIsCompleted() ? LocalDateTime.now().toLocalDate().toString() : null);
                project.setUpdatedAt(LocalDateTime.now().toString());
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
                .filter(r -> formatted.isBlank() || r.getStage().equalsIgnoreCase(formatted))
                .collect(Collectors.toList());
    }

    public RiskDto createRisk(String projectId, RiskDto risk) {
        ProtectProjectDto project = getProjectById(projectId);
        if (risk.getId() == null || risk.getId().isBlank()) {
            risk.setId("rsk-" + UUID.randomUUID().toString().substring(0, 8));
        }
        risk.setProjectId(projectId);
        if (risk.getStage() == null) risk.setStage(project.getCurrentStage());
        if (risk.getCreatedAt() == null) risk.setCreatedAt(LocalDateTime.now().toString());
        project.getRisks().add(0, risk);
        project.setUpdatedAt(LocalDateTime.now().toString());
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
        if (episode.getId() == null || episode.getId().isBlank()) {
            episode.setId("ep-" + UUID.randomUUID().toString().substring(0, 8));
        }
        episode.setProjectId(projectId);
        if (episode.getEpisodeNumber() == null) {
            episode.setEpisodeNumber(project.getEpisodes().size() + 1);
        }
        project.getEpisodes().add(episode);
        project.setUpdatedAt(LocalDateTime.now().toString());
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
                return e;
            }
        }
        throw new ResourceNotFoundException("Episode", "id", epId);
    }

    // Activity
    public List<Map<String, Object>> getActivityLogs(String projectId) {
        return getProjectById(projectId).getRecentActivity();
    }

    private void initDemoProjects() {
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

        TaskDto t1 = new TaskDto();
        t1.setId("tsk-m-1");
        t1.setProjectId(p1.getId());
        t1.setStage("PRE_PRODUCTION");
        t1.setTitle("Principal Script Lock & Scene Breakdown");
        t1.setDescription("Scene-by-scene script breakdown with locked draft v3.2");
        t1.setAssignedTo("Writers Room");
        t1.setStatus("COMPLETED");
        t1.setPriority("CRITICAL");
        t1.setDueDate("2026-08-01");
        t1.setApprovalStatus("APPROVED");
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

        MilestoneDto m1 = new MilestoneDto();
        m1.setId("mls-m-1");
        m1.setProjectId(p1.getId());
        m1.setStage("PRE_PRODUCTION");
        m1.setTitle("Script Locked & Budget Approved");
        m1.setTargetDate("2026-07-25");
        m1.setCompletedDate("2026-07-25");
        m1.setIsCompleted(true);
        p1.getMilestones().add(m1);

        Map<String, Object> act1 = new HashMap<>();
        act1.put("id", "act-m-1");
        act1.put("action", "Project Registered");
        act1.put("performedBy", "Vikramaditya Roy");
        act1.put("details", "Project registered under TrueHunt Protect Producer platform.");
        act1.put("timestamp", "2026-07-10T10:00:00Z");
        p1.getRecentActivity().add(act1);

        projectsMap.put(p1.getId(), p1);

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

        projectsMap.put(p2.getId(), p2);
    }
}

