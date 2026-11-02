package com.truehunt.social.controller;

import com.truehunt.social.dto.*;
import com.truehunt.social.service.ProtectProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/protect-producer")
public class ProtectProducerController {

    private final ProtectProducerService protectProducerService;

    public ProtectProducerController(ProtectProducerService protectProducerService) {
        this.protectProducerService = protectProducerService;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.getCategories()));
    }

    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<List<ProtectProjectDto>>> getProjects(
            @RequestParam(required = false, defaultValue = "") String category
    ) {
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.getProjects(category)));
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<ApiResponse<ProtectProjectDto>> getProjectById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.getProjectById(id)));
    }

    @PostMapping("/projects")
    public ResponseEntity<ApiResponse<ProtectProjectDto>> createProject(@RequestBody ProtectProjectDto projectData) {
        ProtectProjectDto created = protectProducerService.createProject(projectData);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<ApiResponse<ProtectProjectDto>> updateProject(
            @PathVariable String id,
            @RequestBody ProtectProjectDto projectData
    ) {
        ProtectProjectDto updated = protectProducerService.updateProject(id, projectData);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteProject(@PathVariable String id) {
        protectProducerService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Project deleted successfully", "id", id)));
    }

    @PatchMapping("/projects/{id}/stage")
    public ResponseEntity<ApiResponse<ProtectProjectDto>> updateStage(
            @PathVariable String id,
            @RequestBody StageUpdateRequest req
    ) {
        ProtectProjectDto updated = protectProducerService.updateStage(id, req);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    // Tasks
    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<ApiResponse<List<TaskDto>>> getTasks(
            @PathVariable String projectId,
            @RequestParam(required = false, defaultValue = "") String stage
    ) {
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.getTasks(projectId, stage)));
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<ApiResponse<TaskDto>> createTask(
            @PathVariable String projectId,
            @RequestBody TaskDto taskData
    ) {
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.createTask(projectId, taskData)));
    }

    @PutMapping("/projects/{projectId}/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskDto>> updateTask(
            @PathVariable String projectId,
            @PathVariable String taskId,
            @RequestBody TaskDto taskData
    ) {
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.updateTask(projectId, taskId, taskData)));
    }

    // Documents
    @GetMapping("/projects/{projectId}/documents")
    public ResponseEntity<ApiResponse<List<DocumentDto>>> getDocuments(
            @PathVariable String projectId,
            @RequestParam(required = false, defaultValue = "") String stage
    ) {
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.getDocuments(projectId, stage)));
    }

    @PostMapping("/projects/{projectId}/documents")
    public ResponseEntity<ApiResponse<DocumentDto>> createDocument(
            @PathVariable String projectId,
            @RequestBody DocumentDto docData
    ) {
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.createDocument(projectId, docData)));
    }

    @PatchMapping("/projects/{projectId}/documents/{documentId}/verify")
    public ResponseEntity<ApiResponse<DocumentDto>> verifyDocument(
            @PathVariable String projectId,
            @PathVariable String documentId,
            @RequestBody Map<String, Boolean> req
    ) {
        Boolean verified = req != null && Boolean.TRUE.equals(req.get("verified"));
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.verifyDocument(projectId, documentId, verified)));
    }

    // Milestones
    @GetMapping("/projects/{projectId}/milestones")
    public ResponseEntity<ApiResponse<List<MilestoneDto>>> getMilestones(
            @PathVariable String projectId,
            @RequestParam(required = false, defaultValue = "") String stage
    ) {
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.getMilestones(projectId, stage)));
    }

    @PatchMapping("/projects/{projectId}/milestones/{milestoneId}/toggle")
    public ResponseEntity<ApiResponse<MilestoneDto>> toggleMilestone(
            @PathVariable String projectId,
            @PathVariable String milestoneId
    ) {
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.toggleMilestone(projectId, milestoneId)));
    }

    // Risks
    @GetMapping("/projects/{projectId}/risks")
    public ResponseEntity<ApiResponse<List<RiskDto>>> getRisks(
            @PathVariable String projectId,
            @RequestParam(required = false, defaultValue = "") String stage
    ) {
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.getRisks(projectId, stage)));
    }

    @PostMapping("/projects/{projectId}/risks")
    public ResponseEntity<ApiResponse<RiskDto>> createRisk(
            @PathVariable String projectId,
            @RequestBody RiskDto riskData
    ) {
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.createRisk(projectId, riskData)));
    }

    @PutMapping("/projects/{projectId}/risks/{riskId}")
    public ResponseEntity<ApiResponse<RiskDto>> updateRisk(
            @PathVariable String projectId,
            @PathVariable String riskId,
            @RequestBody RiskDto riskData
    ) {
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.updateRisk(projectId, riskId, riskData)));
    }

    // Episodes
    @GetMapping("/projects/{projectId}/episodes")
    public ResponseEntity<ApiResponse<List<EpisodeDto>>> getEpisodes(@PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.getEpisodes(projectId)));
    }

    @PostMapping("/projects/{projectId}/episodes")
    public ResponseEntity<ApiResponse<EpisodeDto>> createEpisode(
            @PathVariable String projectId,
            @RequestBody EpisodeDto epData
    ) {
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.createEpisode(projectId, epData)));
    }

    @PutMapping("/projects/{projectId}/episodes/{epId}")
    public ResponseEntity<ApiResponse<EpisodeDto>> updateEpisode(
            @PathVariable String projectId,
            @PathVariable String epId,
            @RequestBody EpisodeDto epData
    ) {
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.updateEpisode(projectId, epId, epData)));
    }

    // Activity
    @GetMapping("/projects/{projectId}/activity")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActivityLogs(@PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.success(protectProducerService.getActivityLogs(projectId)));
    }
}
