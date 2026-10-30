package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final List<Map<String, Object>> jobs = new ArrayList<>();

    public JobController() {
        initJobs();
    }

    private void initJobs() {
        Map<String, Object> j1 = new HashMap<>();
        j1.put("id", "job-1");
        j1.put("title", "Lead Female Actor — Pan-India Commercial Feature Film");
        j1.put("roleCategory", "Acting");
        j1.put("productionCompany", "Mythri Movie Makers & Sukumar Writings");
        j1.put("location", "Hyderabad & Vizag");
        j1.put("type", "Lead Role / Theatrical Feature");
        j1.put("budgetOrPay", "Competitive Studio Scale (Negotiable)");
        j1.put("postedDate", "1 day ago");
        j1.put("deadline", "Nov 25, 2026");
        j1.put("description", "Auditioning lead female actor (Screen Age: 22-28) for high-octane rural political drama directed by award-winning filmmaker.");
        j1.put("requirements", List.of(
                "Fluent in Telugu and conversational Hindi",
                "Prior theater, short film, or commercial acting reel mandatory",
                "Self-tape audition required with monologues attached"
        ));
        j1.put("department", "Direction / Casting Office");
        j1.put("applicationsCount", 142);
        j1.put("skills", List.of("Telugu Dialogue Delivery", "Method Acting", "Emotional Range"));
        j1.put("verified", true);
        j1.put("experience", "2+ Years");
        jobs.add(j1);

        Map<String, Object> j2 = new HashMap<>();
        j2.put("id", "job-2");
        j2.put("title", "1st Assistant Director (1st AD) — Action Franchise Sequel");
        j2.put("roleCategory", "Direction");
        j2.put("productionCompany", "Hombale Films");
        j2.put("location", "Bengaluru & Mangaluru");
        j2.put("type", "120-Day Production Contract");
        j2.put("budgetOrPay", "₹1,80,000 / month + Location Allowance");
        j2.put("postedDate", "2 days ago");
        j2.put("deadline", "Nov 30, 2026");
        j2.put("description", "Seeking a battle-tested 1st AD with proven experience orchestrating multi-camera heavy stunt units, 500+ background extras, and strict daily call sheets.");
        j2.put("requirements", List.of(
                "Minimum 2 commercial feature films as 1st AD",
                "Mastery of Movie Magic Scheduling and call sheet software",
                "Calm under pressure during high-risk pyrotechnic sequences"
        ));
        j2.put("department", "Direction Department");
        j2.put("applicationsCount", 28);
        j2.put("skills", List.of("Movie Magic Scheduling", "Extra Coordination", "Set Safety Protocols"));
        j2.put("verified", true);
        j2.put("experience", "5+ Years");
        jobs.add(j2);

        Map<String, Object> j3 = new HashMap<>();
        j3.put("id", "job-3");
        j3.put("title", "Director of Photography (DoP) — Independent Festival Feature");
        j3.put("roleCategory", "Cinematography");
        j3.put("productionCompany", "Blue Horizon Indies");
        j3.put("location", "Shimla & Manali, Himachal Pradesh");
        j3.put("type", "Independent Production Contract");
        j3.put("budgetOrPay", "₹4,00,000 fixed + Festival Royalty");
        j3.put("postedDate", "3 days ago");
        j3.put("deadline", "Dec 5, 2026");
        j3.put("description", "Looking for a visionary DoP for a snow-covered psychological drama. Experience with cold-weather shooting and natural ambient light is a must.");
        j3.put("requirements", List.of(
                "Portfolio of indie festival projects",
                "Experience handling camera gear in sub-zero climates",
                "Skilled with lightweight gimbal and hand-held setups"
        ));
        j3.put("department", "Camera Department");
        j3.put("applicationsCount", 45);
        j3.put("skills", List.of("Cold Weather Shooting", "Anamorphic Primes", "Low-light Lighting"));
        j3.put("verified", true);
        j3.put("experience", "4+ Years");
        jobs.add(j3);

        Map<String, Object> j4 = new HashMap<>();
        j4.put("id", "job-4");
        j4.put("title", "Senior Screenplay & Dialogue Writer — OTT Crime Thriller");
        j4.put("roleCategory", "Screenwriting");
        j4.put("productionCompany", "Streamline Entertainment Originals");
        j4.put("location", "Remote / Bengaluru Writers Room");
        j4.put("type", "Writers Room Retainer");
        j4.put("budgetOrPay", "₹6,00,000 - ₹8,50,000 total");
        j4.put("postedDate", "5 days ago");
        j4.put("deadline", "Nov 20, 2026");
        j4.put("description", "Hiring a dialogue writer with a strong ear for authentic regional dialect and gripping courtroom banter for an 8-episode courtroom drama series.");
        j4.put("requirements", List.of(
                "Prior OTT series or feature film credit",
                "Submit 5-page sample courtroom sequence",
                "Available for 6 weeks intensive writers room"
        ));
        j4.put("department", "Writing");
        j4.put("applicationsCount", 67);
        j4.put("skills", List.of("Screenplay Structure", "Dialogue Polish", "Legal Research"));
        j4.put("verified", true);
        j4.put("experience", "3+ Years");
        jobs.add(j4);

        Map<String, Object> j5 = new HashMap<>();
        j5.put("id", "job-5");
        j5.put("title", "Lead VFX Compositor & Matte Painter");
        j5.put("roleCategory", "VFX & Animation");
        j5.put("productionCompany", "PixelForge Studios");
        j5.put("location", "Chennai / Hybrid");
        j5.put("type", "6-Month Project Contract");
        j5.put("budgetOrPay", "₹80,000 - ₹1,20,000 / month");
        j5.put("postedDate", "1 week ago");
        j5.put("deadline", "Dec 1, 2026");
        j5.put("description", "Seeking a senior Nuke compositor with expertise in deep compositing, environment replacement, and CG lighting integration.");
        j5.put("requirements", List.of(
                "Mastery of Foundry Nuke and Photoshop",
                "Minimum 5 years VFX studio experience",
                "Breakdown reel mandatory"
        ));
        j5.put("department", "Post Production");
        j5.put("applicationsCount", 39);
        j5.put("skills", List.of("Nuke Compositing", "Matte Painting", "CGI Integration"));
        j5.put("verified", true);
        j5.put("experience", "5+ Years");
        jobs.add(j5);

        Map<String, Object> j6 = new HashMap<>();
        j6.put("id", "job-6");
        j6.put("title", "Costume Designer & Stylist — Period Drama");
        j6.put("roleCategory", "Makeup & Costume");
        j6.put("productionCompany", "Heritage Filmworks");
        j6.put("location", "Jaipur & Jodhpur");
        j6.put("type", "Full Production Contract");
        j6.put("budgetOrPay", "Negotiable based on experience");
        j6.put("postedDate", "4 days ago");
        j6.put("deadline", "Nov 28, 2026");
        j6.put("description", "Looking for an experienced costume designer with deep knowledge of 19th-century Rajasthani royal attire, hand-loom fabrics, and historical authenticity.");
        j6.put("requirements", List.of(
                "Proven portfolio in historical/period cinema",
                "Extensive artisan and weaver networks in North India",
                "Team leadership skills for a 12-member wardrobe crew"
        ));
        j6.put("department", "Art & Wardrobe");
        j6.put("applicationsCount", 22);
        j6.put("skills", List.of("Period Costumes", "Textile Sourcing", "Wardrobe Management"));
        j6.put("verified", true);
        j6.put("experience", "4+ Years");
        jobs.add(j6);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllJobs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String q
    ) {
        List<Map<String, Object>> result = jobs.stream()
                .filter(j -> {
                    boolean matchCat = category == null || category.isBlank() || category.equalsIgnoreCase("all") ||
                            String.valueOf(j.get("roleCategory")).equalsIgnoreCase(category);
                    boolean matchDept = department == null || department.isBlank() ||
                            String.valueOf(j.get("department")).toLowerCase().contains(department.toLowerCase());
                    boolean matchLoc = location == null || location.isBlank() ||
                            String.valueOf(j.get("location")).toLowerCase().contains(location.toLowerCase());
                    boolean matchQ = q == null || q.isBlank() ||
                            String.valueOf(j.get("title")).toLowerCase().contains(q.toLowerCase()) ||
                            String.valueOf(j.get("description")).toLowerCase().contains(q.toLowerCase());
                    return matchCat && matchDept && matchLoc && matchQ;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getJobById(@PathVariable String id) {
        return jobs.stream()
                .filter(j -> Objects.equals(j.get("id"), id))
                .findFirst()
                .map(j -> ResponseEntity.ok(ApiResponse.success(j)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Opportunity not found")));
    }

    @PostMapping("/{jobId}/applications")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyToJob(
            @PathVariable String jobId,
            @RequestBody(required = false) Map<String, Object> application
    ) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("jobId", jobId);
        res.put("message", "Application submitted successfully! The casting and production team has received your submission.");
        res.put("applicationId", "app-" + System.currentTimeMillis());
        res.put("submittedAt", new Date().toString());
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createJob(@RequestBody Map<String, Object> jobData) {
        Map<String, Object> newJob = new HashMap<>(jobData);
        if (!newJob.containsKey("id")) {
            newJob.put("id", "job-" + System.currentTimeMillis());
        }
        newJob.putIfAbsent("postedDate", "Just now");
        newJob.putIfAbsent("applicationsCount", 0);
        newJob.putIfAbsent("verified", true);
        jobs.add(0, newJob);
        return ResponseEntity.ok(ApiResponse.success(newJob));
    }
}
