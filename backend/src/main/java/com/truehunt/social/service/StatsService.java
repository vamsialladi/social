package com.truehunt.social.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class StatsService {

    public List<Map<String, Object>> getStats() {
        return List.of(
                Map.of("id", "stat-talents", "label", "Talented Professionals", "value", "50K+", "detail", "Verified actors, crew & technicians"),
                Map.of("id", "stat-projects", "label", "Projects & Opportunities", "value", "5K+", "detail", "Auditions, crew calls & openings"),
                Map.of("id", "stat-investors", "label", "Investors & Production Houses", "value", "1K+", "detail", "Producers, studios & financiers"),
                Map.of("id", "stat-connections", "label", "Successful Connections", "value", "25K+", "detail", "Collaborations initiated")
        );
    }
}

