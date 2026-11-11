package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.JobApplicationRequest;
import com.truehunt.social.dto.JobDto;
import com.truehunt.social.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobDto>>> getAllJobs(
            @RequestParam(required = false) Map<String, String> params
    ) {
        List<JobDto> list = jobService.getAll(params);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobDto>> getJobById(@PathVariable String id) {
        JobDto job = jobService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @PostMapping("/{jobId}/applications")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyToJob(
            @PathVariable String jobId,
            @Valid @RequestBody JobApplicationRequest application
    ) {
        Map<String, Object> res = jobService.apply(jobId, application);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JobDto>> createJob(@RequestBody JobDto job) {
        JobDto created = jobService.createJob(job);
        return ResponseEntity.ok(ApiResponse.success(created));
    }
}
