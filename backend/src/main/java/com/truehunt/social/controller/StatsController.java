package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private static final List<Map<String, String>> STATS = List.of(
        Map.of("id", "stat-talents", "label", "Talented Professionals", "value", "50K+", "detail", "Verified actors, crew & technicians"),
        Map.of("id", "stat-projects", "label", "Projects & Opportunities", "value", "5K+", "detail", "Auditions, crew calls & openings"),
        Map.of("id", "stat-investors", "label", "Investors & Production Houses", "value", "1K+", "detail", "Producers, studios & financiers"),
        Map.of("id", "stat-connections", "label", "Successful Connections", "value", "25K+", "detail", "Collaborations initiated")
    );

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(STATS));
    }
}
