package com.truehunt.social.service;

import com.truehunt.social.dto.JobApplicationRequest;
import com.truehunt.social.dto.JobDto;
import com.truehunt.social.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class JobService {

    private final Map<String, JobDto> jobsMap = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> applications = Collections.synchronizedList(new ArrayList<>());

    public JobService() {
        initDefaultJobs();
    }

    public List<JobDto> getAll(Map<String, String> params) {
        String c = params != null ? params.get("craft") : null;
        if (c == null && params != null) c = params.get("category");
        if (c == null && params != null) c = params.get("roleCategory");
        if (c == null && params != null) c = params.get("department");
        final String craft = c;

        final String location = params != null ? params.get("location") : null;

        String s = params != null ? params.get("search") : null;
        if (s == null && params != null) s = params.get("q");
        if (s == null && params != null) s = params.get("query");
        final String search = s;

        return jobsMap.values().stream()
                .filter(j -> craft == null || craft.equalsIgnoreCase("all") || j.getRoleCategory().equalsIgnoreCase(craft))
                .filter(j -> location == null || location.isBlank() || j.getLocation().toLowerCase().contains(location.toLowerCase()))
                .filter(j -> search == null || search.isBlank() || matchesSearch(j, search))
                .collect(Collectors.toList());
    }

    public JobDto getById(String id) {
        JobDto job = jobsMap.get(id);
        if (job != null) {
            return job;
        }
        return jobsMap.values().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));
    }

    public Map<String, Object> apply(String jobId, JobApplicationRequest app) {
        JobDto job = jobsMap.get(jobId);
        if (job != null) {
            job.setApplicationsCount(job.getApplicationsCount() + 1);
        }

        Map<String, Object> record = new HashMap<>();
        record.put("id", "app-" + UUID.randomUUID().toString().substring(0, 8));
        record.put("jobId", jobId);
        record.put("applicantName", app.getApplicantName());
        record.put("applicantEmail", app.getApplicantEmail());
        record.put("appliedAt", new Date().toString());
        record.put("status", "SUBMITTED");
        applications.add(record);

        Map<String, Object> response = new HashMap<>();
        response.put("applicationId", record.get("id"));
        response.put("message", "Application submitted successfully.");
        response.put("status", "SUBMITTED");
        return response;
    }

    public JobDto createJob(JobDto job) {
        if (job.getId() == null || job.getId().isBlank()) {
            job.setId("job-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (job.getApplicationsCount() == null) {
            job.setApplicationsCount(0);
        }
        if (job.getPostedDate() == null) {
            job.setPostedDate("Just now");
        }
        jobsMap.put(job.getId(), job);
        return job;
    }

    private boolean matchesSearch(JobDto j, String term) {
        String lower = term.toLowerCase();
        return (j.getTitle() != null && j.getTitle().toLowerCase().contains(lower))
                || (j.getRoleCategory() != null && j.getRoleCategory().toLowerCase().contains(lower))
                || (j.getProductionCompany() != null && j.getProductionCompany().toLowerCase().contains(lower))
                || (j.getDescription() != null && j.getDescription().toLowerCase().contains(lower));
    }

    private void initDefaultJobs() {
        JobDto j1 = new JobDto();
        j1.setId("job-1");
        j1.setTitle("Lead Female Actor — Pan-India Commercial Feature Film");
        j1.setRoleCategory("Acting");
        j1.setProductionCompany("Mythri Movie Makers & Sukumar Writings");
        j1.setLocation("Hyderabad & Vizag");
        j1.setType("Lead Role / Theatrical Feature");
        j1.setBudgetOrPay("Competitive Studio Scale (Negotiable)");
        j1.setPostedDate("1 day ago");
        j1.setDeadline("Nov 25, 2026");
        j1.setDescription("Auditioning lead female actor (Screen Age: 22-28) for high-octane rural political drama directed by award-winning filmmaker.");
        j1.setRequirements(List.of(
                "Fluent in Telugu and conversational Hindi",
                "Prior theater, short film, or commercial acting reel mandatory",
                "Self-tape audition required with monologues attached"
        ));
        j1.setDepartment("Direction / Casting Office");
        j1.setApplicationsCount(142);
        j1.setSkills(List.of("Telugu Dialogue Delivery", "Method Acting", "Emotional Range"));
        j1.setVerified(true);
        j1.setExperience("2+ Years");
        jobsMap.put(j1.getId(), j1);

        JobDto j2 = new JobDto();
        j2.setId("job-2");
        j2.setTitle("1st Assistant Director (1st AD) — Action Franchise Sequel");
        j2.setRoleCategory("Direction");
        j2.setProductionCompany("Hombale Films");
        j2.setLocation("Bengaluru & Mangaluru");
        j2.setType("120-Day Production Contract");
        j2.setBudgetOrPay("₹1,80,00,000 / month + Location Allowance");
        j2.setPostedDate("2 days ago");
        j2.setDeadline("Nov 30, 2026");
        j2.setDescription("Seeking a battle-tested 1st AD with proven experience orchestrating multi-camera heavy stunt units, 500+ background extras, and strict daily call sheets.");
        j2.setRequirements(List.of(
                "Minimum 2 commercial feature films as 1st AD",
                "Mastery of Movie Magic Scheduling and call sheet software",
                "Calm under pressure during high-risk pyrotechnic sequences"
        ));
        j2.setDepartment("Direction Department");
        j2.setApplicationsCount(68);
        j2.setSkills(List.of("Floor Management", "Movie Magic Scheduling", "Stunt Unit Coordination"));
        j2.setVerified(true);
        j2.setExperience("4+ Years");
        jobsMap.put(j2.getId(), j2);

        JobDto j3 = new JobDto();
        j3.setId("job-3");
        j3.setTitle("Gaffer & Chief Lighting Technician — Cyberpunk OTT Mini-Series");
        j3.setRoleCategory("Cinematography");
        j3.setProductionCompany("Excel Entertainment & Amazon Studios");
        j3.setLocation("Mumbai Studios & Pune Outdoors");
        j3.setType("75-Day Schedule Contract");
        j3.setBudgetOrPay("₹1,20,000 / month");
        j3.setPostedDate("3 days ago");
        j3.setDeadline("Dec 05, 2026");
        j3.setDescription("Hiring Chief Lighting Technician (Gaffer) with extensive knowledge of DMX, CRMX wireless lighting consoles, Astera tubes, and high-speed lighting rigs.");
        j3.setRequirements(List.of(
                "Experience on at least 1 high-end series or major commercial feature",
                "Strong understanding of color temperature, CRI and LED flicker issues",
                "Ability to lead a 15-member lighting crew safely"
        ));
        j3.setDepartment("Cinematography Department");
        j3.setApplicationsCount(39);
        j3.setSkills(List.of("DMX/CRMX Programming", "Astera Rigs", "ARRI SkyPanel", "High-Speed Lighting"));
        j3.setVerified(true);
        j3.setExperience("5+ Years");
        jobsMap.put(j3.getId(), j3);

        JobDto j4 = new JobDto();
        j4.setId("job-4");
        j4.setTitle("Associate Screenplay & Dialogue Writer — Satirical Comedy Feature");
        j4.setRoleCategory("Screenwriting");
        j4.setProductionCompany("Dice Media & Pocket Aces Studios");
        j4.setLocation("Remote / Mumbai Hybrid");
        j4.setType("Project-Based Contract");
        j4.setBudgetOrPay("₹6,00,000 Total Project Fee");
        j4.setPostedDate("4 days ago");
        j4.setDeadline("Dec 10, 2026");
        j4.setDescription("Writers room opening for witty Hindi/Hinglish dialogist and screenplay writer. Project is an observational corporate-political satire.");
        j4.setRequirements(List.of(
                "Submission of 10-page spec script sample required with application",
                "Demonstrated command of contemporary conversational satire",
                "Willingness to attend punch-up writers room sessions"
        ));
        j4.setDepartment("Writers Room");
        j4.setApplicationsCount(94);
        j4.setSkills(List.of("Screenplay Formatting", "Comedy Writing", "Writers Room Collaboration"));
        j4.setVerified(true);
        j4.setExperience("1+ Years");
        jobsMap.put(j4.getId(), j4);
    }
}

