package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/protect-producer")
public class ProtectProducerController {

    private final List<Map<String, Object>> projects = new ArrayList<>();

    public ProtectProducerController() {
        initDemoProjects();
    }

    private void initDemoProjects() {
        Map<String, Object> p1 = new HashMap<>();
        p1.put("id", "proj-movie-1");
        p1.put("category", "MOVIE");
        p1.put("title", "The Red Monsoon");
        p1.put("productionCompany", "Aura Motion Pictures & Mythri Slate");
        p1.put("producerName", "Vikramaditya Roy");
        p1.put("producerEmail", "vikram@aurafilms.in");
        p1.put("synopsis", "A high-stakes political investigative thriller set across the coastal ports of South India.");
        p1.put("status", "IN_PRODUCTION");
        p1.put("currentStage", "PRODUCTION");
        p1.put("stageProgress", 45);
        p1.put("totalBudget", "₹24,00,00,000");
        p1.put("spentBudget", "₹11,40,00,000");
        p1.put("startDate", "2026-08-15");
        p1.put("targetReleaseDate", "2027-04-14");
        p1.put("bannerImageUrl", "/assets/protect-producer/category_movies.webp");
        p1.put("targetRuntime", "158 mins");
        p1.put("createdAt", "2026-07-10T10:00:00Z");
        p1.put("updatedAt", "2026-10-14T12:00:00Z");

        List<Map<String, Object>> tasks = new ArrayList<>();
        Map<String, Object> tsk1 = new HashMap<>();
        tsk1.put("id", "tsk-m-1");
        tsk1.put("projectId", "proj-movie-1");
        tsk1.put("stage", "PRE_PRODUCTION");
        tsk1.put("title", "Principal Script Lock & Scene Breakdown");
        tsk1.put("description", "Scene-by-scene script breakdown with locked draft v3.2");
        tsk1.put("assignedTo", "Writers Room");
        tsk1.put("status", "COMPLETED");
        tsk1.put("priority", "CRITICAL");
        tsk1.put("dueDate", "2026-08-01");
        tsk1.put("approvalStatus", "APPROVED");
        tasks.add(tsk1);
        p1.put("tasks", tasks);

        List<Map<String, Object>> documents = new ArrayList<>();
        Map<String, Object> doc1 = new HashMap<>();
        doc1.put("id", "doc-m-1");
        doc1.put("projectId", "proj-movie-1");
        doc1.put("stage", "PRE_PRODUCTION");
        doc1.put("documentType", "SCRIPT");
        doc1.put("title", "Master Screenplay Draft v3.2 (Locked)");
        doc1.put("fileUrl", "#");
        doc1.put("uploadedBy", "Writer - Karan Mehta");
        doc1.put("verified", true);
        doc1.put("notes", "Signed off by Director and Lead Producer");
        doc1.put("createdAt", "2026-08-01T12:00:00Z");
        documents.add(doc1);
        p1.put("documents", documents);

        List<Map<String, Object>> milestones = new ArrayList<>();
        Map<String, Object> mls1 = new HashMap<>();
        mls1.put("id", "mls-m-1");
        mls1.put("projectId", "proj-movie-1");
        mls1.put("stage", "PRE_PRODUCTION");
        mls1.put("title", "Script Locked & Budget Approved");
        mls1.put("targetDate", "2026-07-25");
        mls1.put("completedDate", "2026-07-25");
        mls1.put("isCompleted", true);
        milestones.add(mls1);
        p1.put("milestones", milestones);

        p1.put("risks", List.of());
        p1.put("recentActivity", List.of(
                Map.of("id", "act-m-1", "action", "Project Registered", "performedBy", "Vikramaditya Roy", "details", "Project registered under TrueHunt Protect Producer platform.", "timestamp", "2026-07-10T10:00:00Z")
        ));
        projects.add(p1);

        Map<String, Object> p2 = new HashMap<>();
        p2.put("id", "proj-serial-1");
        p2.put("category", "SERIAL");
        p2.put("title", "Vamsam: The Dynasty");
        p2.put("productionCompany", "Sun Studio Productions & Gemini Prime");
        p2.put("producerName", "Radhika Sundaram");
        p2.put("producerEmail", "radhika@sunstudio.in");
        p2.put("synopsis", "A multi-generational family saga charting power struggles across three generations.");
        p2.put("status", "IN_PRODUCTION");
        p2.put("currentStage", "PRODUCTION");
        p2.put("stageProgress", 60);
        p2.put("totalBudget", "₹4,80,00,000");
        p2.put("spentBudget", "₹2,90,00,000");
        p2.put("startDate", "2026-06-01");
        p2.put("targetReleaseDate", "2027-06-30");
        p2.put("bannerImageUrl", "/assets/protect-producer/category_serials.webp");
        p2.put("seasonNumber", 1);
        p2.put("totalEpisodes", 120);
        p2.put("tasks", new ArrayList<>());
        p2.put("documents", new ArrayList<>());
        p2.put("milestones", new ArrayList<>());
        p2.put("risks", List.of());
        p2.put("recentActivity", List.of());
        projects.add(p2);

        Map<String, Object> p3 = new HashMap<>();
        p3.put("id", "proj-web-1");
        p3.put("category", "WEB_SERIES");
        p3.put("title", "Chronicles of Deccan");
        p3.put("productionCompany", "Streamline Entertainment Originals");
        p3.put("producerName", "Kabir Sen");
        p3.put("producerEmail", "kabir@streamline.in");
        p3.put("synopsis", "8-episode political crime drama traversing the underground mining empires of 1990s Deccan plateau.");
        p3.put("status", "POST_PRODUCTION");
        p3.put("currentStage", "POST_PRODUCTION");
        p3.put("stageProgress", 75);
        p3.put("totalBudget", "₹18,50,00,000");
        p3.put("spentBudget", "₹14,20,00,000");
        p3.put("startDate", "2026-03-01");
        p3.put("targetReleaseDate", "2027-01-26");
        p3.put("bannerImageUrl", "/assets/protect-producer/category_web_series.webp");
        p3.put("seasonNumber", 1);
        p3.put("totalEpisodes", 8);
        p3.put("tasks", new ArrayList<>());
        p3.put("documents", new ArrayList<>());
        p3.put("milestones", new ArrayList<>());
        p3.put("risks", List.of());
        p3.put("recentActivity", List.of());
        projects.add(p3);

        Map<String, Object> p4 = new HashMap<>();
        p4.put("id", "proj-short-1");
        p4.put("category", "SHORT_FILM");
        p4.put("title", "The Silent Ghat");
        p4.put("productionCompany", "Blue Horizon Indies");
        p4.put("producerName", "Aarav Varma");
        p4.put("producerEmail", "aarav@bluehorizon.in");
        p4.put("synopsis", "A 22-minute visual narrative capturing the sunrise rituals and secret lives along the Ganges.");
        p4.put("status", "COMPLETED");
        p4.put("currentStage", "RELEASE_MANAGEMENT");
        p4.put("stageProgress", 95);
        p4.put("totalBudget", "₹35,00,000");
        p4.put("spentBudget", "₹32,50,000");
        p4.put("startDate", "2026-05-01");
        p4.put("targetReleaseDate", "2026-11-15");
        p4.put("bannerImageUrl", "/assets/protect-producer/category_short_films.webp");
        p4.put("targetRuntime", "22 mins");
        p4.put("tasks", new ArrayList<>());
        p4.put("documents", new ArrayList<>());
        p4.put("milestones", new ArrayList<>());
        p4.put("risks", List.of());
        p4.put("recentActivity", List.of());
        projects.add(p4);
    }

    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getProjects(
            @RequestParam(required = false) String category
    ) {
        if (category == null || category.isBlank() || category.equalsIgnoreCase("all")) {
            return ResponseEntity.ok(ApiResponse.success(projects));
        }
        String formattedCat = category.toUpperCase().replace("-", "_");
        List<Map<String, Object>> filtered = projects.stream()
                .filter(p -> String.valueOf(p.get("category")).toUpperCase().replace("-", "_").equals(formattedCat))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(filtered));
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProjectById(@PathVariable String id) {
        return projects.stream()
                .filter(p -> Objects.equals(p.get("id"), id))
                .findFirst()
                .map(p -> ResponseEntity.ok(ApiResponse.success(p)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Project not found")));
    }

    @PostMapping("/projects")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createProject(@RequestBody Map<String, Object> body) {
        Map<String, Object> newProj = new HashMap<>(body);
        if (!newProj.containsKey("id")) {
            newProj.put("id", "proj-" + System.currentTimeMillis());
        }
        newProj.putIfAbsent("status", "PRE_PRODUCTION");
        newProj.putIfAbsent("currentStage", "PRE_PRODUCTION");
        newProj.putIfAbsent("stageProgress", 10);
        newProj.putIfAbsent("tasks", new ArrayList<>());
        newProj.putIfAbsent("documents", new ArrayList<>());
        newProj.putIfAbsent("milestones", new ArrayList<>());
        newProj.putIfAbsent("risks", new ArrayList<>());
        newProj.putIfAbsent("recentActivity", new ArrayList<>());
        newProj.put("createdAt", new Date().toString());
        newProj.put("updatedAt", new Date().toString());

        projects.add(0, newProj);
        return ResponseEntity.ok(ApiResponse.success(newProj));
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProject(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates
    ) {
        for (int i = 0; i < projects.size(); i++) {
            Map<String, Object> p = projects.get(i);
            if (Objects.equals(p.get("id"), id)) {
                p.putAll(updates);
                p.put("updatedAt", new Date().toString());
                return ResponseEntity.ok(ApiResponse.success(p));
            }
        }
        return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Project not found"));
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteProject(@PathVariable String id) {
        projects.removeIf(p -> Objects.equals(p.get("id"), id));
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("id", id);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @PatchMapping("/projects/{id}/stage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStage(
            @PathVariable String id,
            @RequestBody Map<String, Object> body
    ) {
        for (Map<String, Object> p : projects) {
            if (Objects.equals(p.get("id"), id)) {
                if (body.containsKey("stage")) p.put("currentStage", body.get("stage"));
                if (body.containsKey("progress")) p.put("stageProgress", body.get("progress"));
                if (body.containsKey("status")) p.put("status", body.get("status"));
                p.put("updatedAt", new Date().toString());
                return ResponseEntity.ok(ApiResponse.success(p));
            }
        }
        return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Project not found"));
    }

    @PostMapping("/projects/{id}/tasks")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addTask(
            @PathVariable String id,
            @RequestBody Map<String, Object> task
    ) {
        for (Map<String, Object> p : projects) {
            if (Objects.equals(p.get("id"), id)) {
                List<Map<String, Object>> tasks = (List<Map<String, Object>>) p.computeIfAbsent("tasks", k -> new ArrayList<>());
                Map<String, Object> newTask = new HashMap<>(task);
                if (!newTask.containsKey("id")) {
                    newTask.put("id", "tsk-" + System.currentTimeMillis());
                }
                tasks.add(newTask);
                return ResponseEntity.ok(ApiResponse.success(newTask));
            }
        }
        return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Project not found"));
    }

    @PostMapping("/projects/{id}/documents")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addDocument(
            @PathVariable String id,
            @RequestBody Map<String, Object> doc
    ) {
        for (Map<String, Object> p : projects) {
            if (Objects.equals(p.get("id"), id)) {
                List<Map<String, Object>> docs = (List<Map<String, Object>>) p.computeIfAbsent("documents", k -> new ArrayList<>());
                Map<String, Object> newDoc = new HashMap<>(doc);
                if (!newDoc.containsKey("id")) {
                    newDoc.put("id", "doc-" + System.currentTimeMillis());
                }
                docs.add(newDoc);
                return ResponseEntity.ok(ApiResponse.success(newDoc));
            }
        }
        return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Project not found"));
    }

    @PostMapping("/projects/{id}/milestones")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addMilestone(
            @PathVariable String id,
            @RequestBody Map<String, Object> mls
    ) {
        for (Map<String, Object> p : projects) {
            if (Objects.equals(p.get("id"), id)) {
                List<Map<String, Object>> milestones = (List<Map<String, Object>>) p.computeIfAbsent("milestones", k -> new ArrayList<>());
                Map<String, Object> newMls = new HashMap<>(mls);
                if (!newMls.containsKey("id")) {
                    newMls.put("id", "mls-" + System.currentTimeMillis());
                }
                milestones.add(newMls);
                return ResponseEntity.ok(ApiResponse.success(newMls));
            }
        }
        return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Project not found"));
    }
}
