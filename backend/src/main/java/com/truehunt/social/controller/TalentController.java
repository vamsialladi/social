package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/talents")
public class TalentController {

    private final List<Map<String, Object>> talents = new ArrayList<>();

    public TalentController() {
        initTalents();
    }

    private void initTalents() {
        Map<String, Object> t1 = new HashMap<>();
        t1.put("id", "t-1");
        t1.put("name", "Aarav Varma");
        t1.put("role", "Cinematographer (DoP)");
        t1.put("category", "Cinematography");
        t1.put("location", "Mumbai, India");
        t1.put("experience", "8 Years");
        t1.put("avatar", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80");
        t1.put("coverImage", "https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=1200&q=80");
        t1.put("bio", "Award-winning cinematographer specializing in low-light anamorphic photography and psychological thrillers. Member of ISC.");
        t1.put("skills", List.of("ARRI Alexa Mini LF", "RED V-Raptor", "Anamorphic Lenses", "Color Grading (DaVinci)", "Steadicam"));
        t1.put("credits", List.of(
                Map.of("title", "Shadows of Malabar", "role", "Director of Photography", "year", "2025", "format", "Feature Film (Netflix)"),
                Map.of("title", "The Silent Ghat", "role", "DoP", "year", "2024", "format", "Independent Festival Film"),
                Map.of("title", "Monsoon Raga", "role", "2nd Unit Cameraman", "year", "2023", "format", "Theatrical Release")
        ));
        t1.put("showreel", "https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        t1.put("availability", "Available from Nov 2026");
        t1.put("verified", true);
        t1.put("connectionsCount", 524);
        t1.put("followersCount", 1840);
        t1.put("headline", "Award-Winning Cinematographer (DoP) | Member of ISC | Low-light & Anamorphic Specialist");
        t1.put("company", "Aura Cinema Works");
        t1.put("industry", "Motion Pictures & Film");
        t1.put("websiteUrl", "https://aaravvarma.cinematography.in");
        t1.put("experiences", List.of(
                Map.of("id", "exp-1", "title", "Director of Photography (DoP)", "company", "Aura Cinema Works & Netflix India", "employmentType", "Contract", "location", "Mumbai, India", "startDate", "Jan 2024", "endDate", "Present", "isCurrent", true, "description", "Leading principal cinematography for high-budget streaming feature films.")
        ));
        t1.put("educations", List.of(
                Map.of("id", "edu-1", "institution", "Film and Television Institute of India (FTII)", "degree", "Postgraduate Diploma", "fieldOfStudy", "Cinematography & Film Production", "startDate", "2015", "endDate", "2018", "grade", "Distinction")
        ));
        t1.put("projects", List.of(
                Map.of("id", "proj-1", "title", "Shadows of Malabar (Feature Film)", "description", "60-day feature shoot across rain-soaked Western Ghats with extensive night sequences.", "technologies", "ARRI Alexa LF, Master Anamorphics", "projectUrl", "https://truehunt.in/projects/malabar", "startDate", "2024", "endDate", "2025")
        ));
        t1.put("posts", List.of(
                Map.of("id", "post-1", "authorId", "t-1", "authorName", "Aarav Varma", "authorAvatar", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80", "authorRole", "Cinematographer (DoP)", "content", "Wrapped principal photography on our 45-day night schedule!", "likesCount", 142, "commentsCount", 28, "createdAt", "2026-10-14T10:00:00Z")
        ));
        talents.add(t1);

        Map<String, Object> t2 = new HashMap<>();
        t2.put("id", "t-2");
        t2.put("name", "Meera Nambiar");
        t2.put("role", "Lead Actress & Theatre Artist");
        t2.put("category", "Acting");
        t2.put("location", "Kochi & Bengaluru, India");
        t2.put("experience", "5 Years");
        t2.put("avatar", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=400&q=80");
        t2.put("coverImage", "https://images.unsplash.com/photo-1514306191717-452ec28c7814?auto=format&fit=crop&w=1200&q=80");
        t2.put("bio", "Method actress trained at National School of Drama. Fluent in Malayalam, Tamil, Hindi, and English.");
        t2.put("skills", List.of("Method Acting", "Kalaripayattu", "Classical Dance", "Voice Modulation"));
        t2.put("credits", List.of(
                Map.of("title", "Neelakurinji Blooming", "role", "Lead Protagonist", "year", "2025", "format", "Critically Acclaimed Feature")
        ));
        t2.put("availability", "Open for Auditions");
        t2.put("verified", true);
        t2.put("connectionsCount", 890);
        t2.put("followersCount", 2400);
        talents.add(t2);

        Map<String, Object> t3 = new HashMap<>();
        t3.put("id", "t-3");
        t3.put("name", "Kabir Sen");
        t3.put("role", "Director & Screenwriter");
        t3.put("category", "Direction");
        t3.put("location", "Kolkata & Mumbai, India");
        t3.put("experience", "11 Years");
        t3.put("avatar", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=400&q=80");
        t3.put("coverImage", "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?auto=format&fit=crop&w=1200&q=80");
        t3.put("bio", "Writer-director focusing on human-centric dramas, crime noir, and socio-political satire.");
        t3.put("skills", List.of("Feature Directing", "Script Doctoring", "Actor Workshops", "Non-linear Storytelling"));
        t3.put("credits", List.of(
                Map.of("title", "Midnight at Howrah", "role", "Director & Screenplay", "year", "2024", "format", "Theatrical Feature")
        ));
        t3.put("availability", "Developing Pitch Slate");
        t3.put("verified", true);
        t3.put("connectionsCount", 1340);
        talents.add(t3);

        Map<String, Object> t4 = new HashMap<>();
        t4.put("id", "t-4");
        t4.put("name", "Pooja Deshmukh");
        t4.put("role", "Chief Editor & Colorist");
        t4.put("category", "Editing");
        t4.put("location", "Pune & Mumbai, India");
        t4.put("experience", "7 Years");
        t4.put("avatar", "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=400&q=80");
        t4.put("coverImage", "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?auto=format&fit=crop&w=1200&q=80");
        t4.put("bio", "High-precision rhythm editor with experience in commercial action cinema and documentary formats.");
        t4.put("skills", List.of("Avid Media Composer", "Adobe Premiere Pro", "DaVinci Resolve", "DCP Mastering"));
        t4.put("credits", List.of(
                Map.of("title", "Velocity Rush", "role", "Head Editor", "year", "2025", "format", "Action Blockbuster")
        ));
        t4.put("availability", "Accepting Projects for Q1 2027");
        t4.put("verified", true);
        t4.put("connectionsCount", 610);
        talents.add(t4);

        Map<String, Object> t5 = new HashMap<>();
        t5.put("id", "t-5");
        t5.put("name", "Vikramaditya Roy");
        t5.put("role", "Music Director & Score Composer");
        t5.put("category", "Music");
        t5.put("location", "Chennai, India");
        t5.put("experience", "9 Years");
        t5.put("avatar", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80");
        t5.put("coverImage", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=1200&q=80");
        t5.put("bio", "Orchestral film composer blending traditional South Asian ragas with modern Hollywood brass.");
        t5.put("skills", List.of("Orchestral Composition", "Logic Pro X", "Cubase", "Folk Fusion", "Dolby Atmos Mix"));
        t5.put("credits", List.of(
                Map.of("title", "The Kingdom Beneath", "role", "Original Background Score", "year", "2025", "format", "Pan-India Period Epic")
        ));
        t5.put("availability", "Open for Feature Film Scoring");
        t5.put("verified", true);
        t5.put("connectionsCount", 1520);
        talents.add(t5);

        Map<String, Object> t6 = new HashMap<>();
        t6.put("id", "t-6");
        t6.put("name", "Divya Sundaram");
        t6.put("role", "Sound Designer & Sync Sound Recordist");
        t6.put("category", "Sound Design");
        t6.put("location", "Hyderabad, India");
        t6.put("experience", "6 Years");
        t6.put("avatar", "https://images.unsplash.com/photo-1580489944761-15a19d654956?auto=format&fit=crop&w=400&q=80");
        t6.put("coverImage", "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?auto=format&fit=crop&w=1200&q=80");
        t6.put("bio", "Sound designer obsessed with organic foley and immersive acoustic environments.");
        t6.put("skills", List.of("Pro Tools Ultimate", "Sound Devices 833", "Ambisonics", "Foley Recording"));
        t6.put("credits", List.of(
                Map.of("title", "Whispers of Araku", "role", "Sync Sound & Sound Designer", "year", "2025", "format", "Feature Film")
        ));
        t6.put("availability", "Available Immediately");
        t6.put("verified", false);
        t6.put("connectionsCount", 412);
        talents.add(t6);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllTalents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String location
    ) {
        List<Map<String, Object>> result = talents.stream()
                .filter(t -> {
                    boolean matchCat = category == null || category.isBlank() || category.equalsIgnoreCase("all") ||
                            String.valueOf(t.get("category")).equalsIgnoreCase(category);
                    boolean matchLoc = location == null || location.isBlank() ||
                            String.valueOf(t.get("location")).toLowerCase().contains(location.toLowerCase());
                    boolean matchQ = q == null || q.isBlank() ||
                            String.valueOf(t.get("name")).toLowerCase().contains(q.toLowerCase()) ||
                            String.valueOf(t.get("role")).toLowerCase().contains(q.toLowerCase());
                    return matchCat && matchLoc && matchQ;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTalentById(@PathVariable String id) {
        return talents.stream()
                .filter(t -> Objects.equals(t.get("id"), id))
                .findFirst()
                .map(t -> ResponseEntity.ok(ApiResponse.success(t)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "Talent not found")));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchTalents(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location
    ) {
        return getAllTalents(category, q, location);
    }

    @PostMapping("/connect")
    public ResponseEntity<ApiResponse<Map<String, Object>>> connect(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Connection request sent successfully.");
        res.put("timestamp", new Date().toString());
        return ResponseEntity.ok(ApiResponse.success(res));
    }
}
