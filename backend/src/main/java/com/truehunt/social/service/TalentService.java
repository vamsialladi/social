package com.truehunt.social.service;

import com.truehunt.social.dto.EducationDto;
import com.truehunt.social.dto.ExperienceDto;
import com.truehunt.social.dto.ProjectDto;
import com.truehunt.social.dto.TalentDto;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.model.PostEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserProfileRelationshipEntity;
import com.truehunt.social.repository.PostRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserProfileRelationshipRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TalentService {

    private static final List<List<String>> CRAFT_EQUIVALENCE_GROUPS = List.of(
            List.of("act", "cast"),
            List.of("cinemat", "dop", "camera"),
            List.of("direct"),
            List.of("screenwrit", "storywrit", "dialoguewrit", "scriptwrit", "writer"),
            List.of("music", "composer", "score"),
            List.of("sound", "audiograph", "dubbing"),
            List.of("edit", "colorist"),
            List.of("artdirect", "productiondesign"),
            List.of("vfx", "animation", "cgi"),
            List.of("makeup", "hairstylist"),
            List.of("costume", "wardrobe"),
            List.of("dance", "choreograph"),
            List.of("production", "producer", "productionexecutive")
    );

    private final Map<String, TalentDto> talentMap = new ConcurrentHashMap<>();
    private final ProfileRepository profileRepository;
    private final PostRepository postRepository;
    private final UserProfileRelationshipRepository relationshipRepository;
    private final ObjectProvider<ConnectionService> connectionServiceProvider;

    public TalentService(ProfileRepository profileRepository) {
        this(profileRepository, null, null, null);
    }

    @Autowired
    public TalentService(ProfileRepository profileRepository,
                         PostRepository postRepository,
                         UserProfileRelationshipRepository relationshipRepository,
                         ObjectProvider<ConnectionService> connectionServiceProvider) {
        this.profileRepository = profileRepository;
        this.postRepository = postRepository;
        this.relationshipRepository = relationshipRepository;
        this.connectionServiceProvider = connectionServiceProvider;
        initDefaultTalents();
    }

    public List<TalentDto> getAll(Map<String, String> params) {
        return getAll(params, null);
    }

    public List<TalentDto> getAll(Map<String, String> params, String callerIdentifier) {
        String c = params != null ? params.get("category") : null;
        if (c == null && params != null) c = params.get("craft");
        if (c == null && params != null) c = params.get("role");
        final String category = c;

        String sc = params != null ? params.get("subCraft") : null;
        if (sc == null && params != null) sc = params.get("subcraft");
        final String subCraft = sc;

        final String location = params != null ? params.get("location") : null;

        String s = params != null ? params.get("search") : null;
        if (s == null && params != null) s = params.get("q");
        if (s == null && params != null) s = params.get("term");
        if (s == null && params != null) s = params.get("query");
        final String search = s;

        return getCombinedTalents(callerIdentifier).stream()
                .filter(t -> isCraftMatching(t, category))
                .filter(t -> isSubCraftMatching(t, subCraft))
                .filter(t -> isLocationMatching(t, location))
                .filter(t -> search == null || search.isBlank() || matchesSearch(t, search))
                .collect(Collectors.toList());
    }

    public TalentDto getById(String id) {
        return getById(id, null);
    }

    public TalentDto getById(String id, String callerIdentifier) {
        return getCombinedTalents(callerIdentifier).stream()
                .filter(t -> Objects.equals(t.getId(), id)
                        || (t.getUserId() != null && t.getUserId().equalsIgnoreCase(id))
                        || (t.getEmail() != null && t.getEmail().equalsIgnoreCase(id)))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Talent", "id", id));
    }

    public List<TalentDto> search(String term, String category, String location) {
        return getCombinedTalents(null).stream()
                .filter(t -> term == null || term.isBlank() || matchesSearch(t, term))
                .filter(t -> isCraftMatching(t, category))
                .filter(t -> isLocationMatching(t, location))
                .collect(Collectors.toList());
    }

    public void addTalent(TalentDto talent) {
        talentMap.put(talent.getId(), talent);
    }

    private List<TalentDto> getCombinedTalents(String callerIdentifier) {
        Map<String, TalentDto> map = new LinkedHashMap<>();

        // Add pre-configured default talents
        for (TalentDto t : talentMap.values()) {
            map.put(t.getId(), copyWithCallerState(t, callerIdentifier));
        }

        // Add registered database profiles
        if (profileRepository != null) {
            List<PostEntity> allPosts = postRepository != null ? postRepository.findAllByOrderByCreatedAtDesc() : List.of();
            List<ProfileEntity> registeredProfiles = profileRepository.findAll().stream()
                    .filter(profile -> profile.getName() != null && !profile.getName().isBlank())
                    .toList();
            Set<String> registeredNames = registeredProfiles.stream()
                    .map(profile -> normalizeName(profile.getName()))
                    .collect(Collectors.toSet());
            map.entrySet().removeIf(entry -> registeredNames.contains(normalizeName(entry.getValue().getName())));

            for (ProfileEntity p : registeredProfiles) {
                String uid = p.getUserId() != null && !p.getUserId().isBlank() ? p.getUserId() : String.valueOf(p.getId());
                if (!map.containsKey(uid)) {
                    TalentDto dto = new TalentDto();
                    dto.setId(uid);
                    dto.setUserId(uid);
                    dto.setEmail(p.getEmail());
                    dto.setName(p.getName());
                    String pCraft = p.getPrimaryCraft() != null && !p.getPrimaryCraft().isBlank()
                            ? p.getPrimaryCraft() : "Cast (Actors)";
                    String sCraft = p.getSubCraft() != null && !p.getSubCraft().isBlank()
                            ? p.getSubCraft() : (pCraft.toLowerCase().contains("act") ? "Lead Actor" : pCraft);
                    dto.setPrimaryCraft(pCraft);
                    dto.setCategory(pCraft);
                    dto.setSubCraft(sCraft);
                    dto.setRole(sCraft);
                    dto.setHeadline(p.getHeadline() != null && !p.getHeadline().isBlank()
                            ? p.getHeadline() : (sCraft + " • " + pCraft));
                    dto.setLocation(p.getAddress() != null && !p.getAddress().isBlank() ? p.getAddress() : "Hyderabad, India");
                    dto.setExperience(p.getExperience() != null ? p.getExperience() : "3+ Years");
                    dto.setAvatar(p.getProfileImageUrl());
                    dto.setCoverImage(p.getCoverImageUrl());
                    dto.setCompany(p.getCompany());
                    dto.setIndustry(p.getIndustry());
                    dto.setWebsiteUrl(p.getWebsiteUrl());
                    dto.setAvailability(p.getAvailability() != null ? p.getAvailability() : "Available for New Projects");
                    if (p.getVideoUrls() != null && !p.getVideoUrls().isEmpty()) {
                        dto.setShowreel(p.getVideoUrls().get(0));
                    }
                    String bio = p.getBio() != null ? p.getBio() : p.getHighlights();
                    dto.setBio(bio);
                    dto.setSkills(p.getTalents());
                    dto.setVerified(true);

                    // Map experiences
                    if (p.getExperiences() != null) {
                        List<ExperienceDto> exps = new ArrayList<>();
                        for (Map<String, Object> expMap : p.getExperiences()) {
                            ExperienceDto exp = new ExperienceDto();
                            exp.setId((String) expMap.get("id"));
                            String title = (String) expMap.getOrDefault("title", expMap.getOrDefault("workedAs", expMap.get("projectName")));
                            String comp = (String) expMap.getOrDefault("company", expMap.get("organizationName"));
                            exp.setTitle(title);
                            exp.setCompany(comp);
                            exp.setProjectName((String) expMap.getOrDefault("projectName", title));
                            exp.setOrganizationName((String) expMap.getOrDefault("organizationName", comp));
                            exp.setWorkedAs((String) expMap.getOrDefault("workedAs", title));
                            exp.setLocation((String) expMap.get("location"));
                            exp.setStartDate((String) expMap.get("startDate"));
                            exp.setEndDate((String) expMap.get("endDate"));
                            exp.setDescription((String) expMap.getOrDefault("description", expMap.get("highlights")));
                            exp.setHighlights((String) expMap.getOrDefault("highlights", expMap.get("description")));
                            exp.setEmploymentType((String) expMap.get("employmentType"));
                            String logo = (String) expMap.getOrDefault("organizationLogoUrl", expMap.get("companyLogoUrl"));
                            exp.setOrganizationLogoUrl(logo);
                            exp.setCompanyLogoUrl(logo);
                            exps.add(exp);
                        }
                        dto.setExperiences(exps);
                    }

                    // Map educations
                    if (p.getEducations() != null) {
                        List<EducationDto> edus = new ArrayList<>();
                        for (Map<String, Object> eduMap : p.getEducations()) {
                            EducationDto edu = new EducationDto();
                            edu.setId((String) eduMap.get("id"));
                            edu.setInstitution((String) eduMap.get("institution"));
                            edu.setDegree((String) eduMap.get("degree"));
                            edu.setFieldOfStudy((String) eduMap.get("fieldOfStudy"));
                            edu.setStartDate((String) eduMap.get("startDate"));
                            edu.setEndDate((String) eduMap.get("endDate"));
                            edu.setGrade((String) eduMap.get("grade"));
                            edu.setDescription((String) eduMap.get("description"));
                            edus.add(edu);
                        }
                        dto.setEducations(edus);
                    }

                    // Map projects
                    if (p.getProjects() != null) {
                        List<ProjectDto> projs = new ArrayList<>();
                        for (Map<String, Object> projMap : p.getProjects()) {
                            ProjectDto proj = new ProjectDto();
                            proj.setId((String) projMap.get("id"));
                            proj.setTitle((String) projMap.get("title"));
                            proj.setDescription((String) projMap.get("description"));
                            proj.setTechnologies((String) projMap.get("technologies"));
                            proj.setProjectUrl((String) projMap.get("projectUrl"));
                            proj.setStartDate((String) projMap.get("startDate"));
                            proj.setEndDate((String) projMap.get("endDate"));
                            projs.add(proj);
                        }
                        dto.setProjects(projs);
                    }

                    // Populate posts belonging to this profile
                    if (!allPosts.isEmpty()) {
                        Set<String> authorKeys = new HashSet<>();
                        authorKeys.add(uid.toLowerCase(Locale.ROOT));
                        if (p.getEmail() != null && !p.getEmail().isBlank()) {
                            authorKeys.add(p.getEmail().trim().toLowerCase(Locale.ROOT));
                        }
                        if (p.getId() != null) {
                            authorKeys.add(String.valueOf(p.getId()));
                        }
                        List<Map<String, Object>> matchingPosts = allPosts.stream()
                                .filter(post -> post.getAuthorId() != null && authorKeys.contains(post.getAuthorId().trim().toLowerCase(Locale.ROOT)))
                                .map(PostEntity::toDto)
                                .collect(Collectors.toList());
                        dto.setPosts(matchingPosts);
                    }

                    applyCallerConnectionState(dto, callerIdentifier);
                    map.put(uid, dto);
                }
            }
        }

        return new ArrayList<>(map.values());
    }

    private TalentDto copyWithCallerState(TalentDto src, String callerIdentifier) {
        if (callerIdentifier == null || callerIdentifier.isBlank()) {
            return src;
        }
        TalentDto copy = new TalentDto();
        copy.setId(src.getId());
        copy.setUserId(src.getUserId());
        copy.setEmail(src.getEmail());
        copy.setName(src.getName());
        copy.setRole(src.getRole());
        copy.setCategory(src.getCategory());
        copy.setPrimaryCraft(src.getPrimaryCraft());
        copy.setSubCraft(src.getSubCraft());
        copy.setLocation(src.getLocation());
        copy.setExperience(src.getExperience());
        copy.setAvatar(src.getAvatar());
        copy.setCoverImage(src.getCoverImage());
        copy.setBio(src.getBio());
        copy.setSkills(src.getSkills());
        copy.setCredits(src.getCredits());
        copy.setShowreel(src.getShowreel());
        copy.setAvailability(src.getAvailability());
        copy.setVerified(src.getVerified());
        copy.setConnectionsCount(src.getConnectionsCount());
        copy.setFollowersCount(src.getFollowersCount());
        copy.setHeadline(src.getHeadline());
        copy.setCompany(src.getCompany());
        copy.setIndustry(src.getIndustry());
        copy.setWebsiteUrl(src.getWebsiteUrl());
        copy.setExperiences(src.getExperiences());
        copy.setEducations(src.getEducations());
        copy.setProjects(src.getProjects());
        copy.setPosts(src.getPosts());
        applyCallerConnectionState(copy, callerIdentifier);
        return copy;
    }

    private void applyCallerConnectionState(TalentDto dto, String callerIdentifier) {
        ConnectionService connService = connectionServiceProvider != null ? connectionServiceProvider.getIfAvailable() : null;
        if (connService == null || callerIdentifier == null || callerIdentifier.isBlank()) {
            return;
        }
        Set<String> callerIds = connService.resolveAllUserIdentifiers(callerIdentifier);
        Set<String> targetIds = new LinkedHashSet<>(connService.resolveAllUserIdentifiers(dto.getUserId()));
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            targetIds.addAll(connService.resolveAllUserIdentifiers(dto.getEmail()));
        }

        if (relationshipRepository != null && !callerIds.isEmpty() && !targetIds.isEmpty()) {
            List<UserProfileRelationshipEntity> rels = relationshipRepository.findAll();
            boolean connected = false;
            boolean outgoingPending = false;
            boolean incomingPending = false;
            for (UserProfileRelationshipEntity r : rels) {
                String owner = r.getOwnerUserId() != null ? r.getOwnerUserId().trim().toLowerCase(Locale.ROOT) : "";
                String target = r.getTargetProfileId() != null ? r.getTargetProfileId().trim().toLowerCase(Locale.ROOT) : "";
                boolean matchPair = (callerIds.contains(owner) && targetIds.contains(target))
                        || (targetIds.contains(owner) && callerIds.contains(target));
                if (!matchPair) {
                    continue;
                }
                if (r.isConnected() || "ACCEPTED".equalsIgnoreCase(r.getStatus())) {
                    connected = true;
                } else if ("PENDING".equalsIgnoreCase(r.getStatus())) {
                    String sender = r.getSenderUserId() != null ? r.getSenderUserId().trim().toLowerCase(Locale.ROOT) : owner;
                    if (callerIds.contains(sender)) {
                        outgoingPending = true;
                    } else {
                        incomingPending = true;
                    }
                }
            }
            dto.setIsConnected(connected);
            dto.setIsOutgoingPending(outgoingPending);
            dto.setIsIncomingPending(incomingPending);
            dto.setIsPending(outgoingPending || incomingPending);
            dto.setConnectionStatus(connected ? "CONNECTED" : (outgoingPending ? "PENDING_OUTGOING" : (incomingPending ? "PENDING_INCOMING" : "NONE")));
        }
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private boolean isCraftMatching(TalentDto t, String craftQuery) {
        if (craftQuery == null || craftQuery.isBlank() || craftQuery.equalsIgnoreCase("all")) {
            return true;
        }
        String q = craftQuery.toLowerCase().replaceAll("[^a-z0-9]", "");
        List<String> crafts = new ArrayList<>();
        if (t.getCategory() != null) crafts.add(t.getCategory());
        if (t.getPrimaryCraft() != null) crafts.add(t.getPrimaryCraft());
        if (t.getRole() != null) crafts.add(t.getRole());
        if (t.getHeadline() != null) crafts.add(t.getHeadline());

        List<String> normalizedCrafts = crafts.stream()
                .map(c -> c.toLowerCase().replaceAll("[^a-z0-9]", ""))
                .toList();

        for (String norm : normalizedCrafts) {
            if (norm.equals(q) || norm.contains(q) || q.contains(norm)) {
                return true;
            }
        }

        for (List<String> group : CRAFT_EQUIVALENCE_GROUPS) {
            boolean queryInGroup = group.stream().anyMatch(q::contains);
            if (queryInGroup) {
                for (String norm : normalizedCrafts) {
                    if (group.stream().anyMatch(norm::contains)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isSubCraftMatching(TalentDto t, String subCraftQuery) {
        if (subCraftQuery == null || subCraftQuery.isBlank() || subCraftQuery.equalsIgnoreCase("all")) {
            return true;
        }
        String q = subCraftQuery.toLowerCase().replaceAll("[^a-z0-9]", "");
        List<String> subCrafts = new ArrayList<>();
        if (t.getSubCraft() != null) subCrafts.add(t.getSubCraft());
        if (t.getRole() != null) subCrafts.add(t.getRole());
        if (t.getHeadline() != null) subCrafts.add(t.getHeadline());
        if (t.getSkills() != null) subCrafts.addAll(t.getSkills());

        for (String sc : subCrafts) {
            String norm = sc.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (norm.equals(q) || norm.contains(q) || q.contains(norm)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLocationMatching(TalentDto t, String locationQuery) {
        if (locationQuery == null || locationQuery.isBlank() || locationQuery.equalsIgnoreCase("all")) {
            return true;
        }
        String locLower = locationQuery.toLowerCase().trim();
        if (t.getLocation() != null && t.getLocation().toLowerCase().contains(locLower)) {
            return true;
        }
        if (t.getExperiences() != null) {
            for (ExperienceDto exp : t.getExperiences()) {
                if (exp.getLocation() != null && exp.getLocation().toLowerCase().contains(locLower)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesSearch(TalentDto t, String term) {
        if (term == null || term.isBlank()) return true;
        String lower = term.toLowerCase().trim();
        return (t.getName() != null && t.getName().toLowerCase().contains(lower))
                || (t.getSubCraft() != null && t.getSubCraft().toLowerCase().contains(lower))
                || (t.getPrimaryCraft() != null && t.getPrimaryCraft().toLowerCase().contains(lower))
                || (t.getRole() != null && t.getRole().toLowerCase().contains(lower))
                || (t.getHeadline() != null && t.getHeadline().toLowerCase().contains(lower))
                || (t.getCategory() != null && t.getCategory().toLowerCase().contains(lower))
                || (t.getBio() != null && t.getBio().toLowerCase().contains(lower))
                || (t.getLocation() != null && t.getLocation().toLowerCase().contains(lower))
                || (t.getCompany() != null && t.getCompany().toLowerCase().contains(lower))
                || (t.getSkills() != null && t.getSkills().stream().anyMatch(s -> s.toLowerCase().contains(lower)));
    }

    private void initDefaultTalents() {
        // Talent 1: Aarav Varma (Cinematographer)
        TalentDto t1 = new TalentDto();
        t1.setId("t-1");
        t1.setName("Aarav Varma");
        t1.setRole("Cinematographer (DoP)");
        t1.setCategory("Cinematography");
        t1.setPrimaryCraft("Cinematography");
        t1.setSubCraft("Director of Photography (DoP)");
        t1.setLocation("Mumbai, India");
        t1.setExperience("8 Years");
        t1.setAvatar("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80");
        t1.setCoverImage("https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=1200&q=80");
        t1.setBio("Award-winning cinematographer specializing in low-light anamorphic photography and psychological thrillers. Member of ISC.");
        t1.setSkills(List.of("ARRI Alexa Mini LF", "RED V-Raptor", "Anamorphic Lenses", "Color Grading (DaVinci)", "Steadicam"));
        t1.setShowreel("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        t1.setAvailability("Available from Nov 2026");
        t1.setVerified(true);
        t1.setConnectionsCount(524);
        t1.setFollowersCount(1840);
        t1.setHeadline("Award-Winning Cinematographer (DoP) | Member of ISC | Low-light & Anamorphic Specialist");
        t1.setCompany("Aura Cinema Works");
        t1.setIndustry("Motion Pictures & Film");
        t1.setWebsiteUrl("https://aaravvarma.cinematography.in");
        t1.setCredits(List.of(
                Map.of("title", "Shadows of Malabar", "role", "Director of Photography", "year", "2025", "format", "Feature Film (Netflix)"),
                Map.of("title", "The Silent Ghat", "role", "DoP", "year", "2024", "format", "Independent Festival Film")
        ));
        ExperienceDto exp1 = new ExperienceDto();
        exp1.setId("exp-t1-1");
        exp1.setTitle("Director of Photography (DoP)");
        exp1.setCompany("Aura Cinema Works & Netflix India");
        exp1.setEmploymentType("Contract");
        exp1.setLocation("Mumbai, India");
        exp1.setStartDate("Jan 2024");
        exp1.setEndDate("Present");
        exp1.setIsCurrent(true);
        exp1.setDescription("Leading principal cinematography for high-budget streaming feature films.");
        t1.setExperiences(List.of(exp1));
        EducationDto edu1 = new EducationDto();
        edu1.setId("edu-t1-1");
        edu1.setInstitution("Film and Television Institute of India (FTII)");
        edu1.setDegree("Postgraduate Diploma");
        edu1.setFieldOfStudy("Cinematography & Film Production");
        edu1.setStartDate("2015");
        edu1.setEndDate("2018");
        edu1.setGrade("Distinction");
        t1.setEducations(List.of(edu1));
        talentMap.put(t1.getId(), t1);

        // Talent 2: Meera Nambiar (Acting - Lead Actress)
        TalentDto t2 = new TalentDto();
        t2.setId("t-2");
        t2.setName("Meera Nambiar");
        t2.setRole("Lead Actress & Theatre Artist");
        t2.setCategory("Acting");
        t2.setPrimaryCraft("Cast (Actors)");
        t2.setSubCraft("Lead Actress");
        t2.setLocation("Kochi & Bengaluru, India");
        t2.setExperience("5 Years");
        t2.setAvatar("https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=400&q=80");
        t2.setCoverImage("https://images.unsplash.com/photo-1514306191717-452ec28c7814?auto=format&fit=crop&w=1200&q=80");
        t2.setBio("Method actress trained at National School of Drama. Fluent in Malayalam, Tamil, Hindi, and English. Experienced in dramatic leads and intense character roles.");
        t2.setSkills(List.of("Lead Actress", "Method Acting", "Kalaripayattu", "Classical Dance (Bharatanatyam)", "Voice Modulation", "Dialect Coaching"));
        t2.setAvailability("Available Immediately");
        t2.setVerified(true);
        t2.setConnectionsCount(412);
        t2.setFollowersCount(3200);
        t2.setHeadline("Lead Actress & Theatre Artist | NSD Alumna | Multilingual Performer");
        t2.setCredits(List.of(
                Map.of("title", "Kayal", "role", "Lead Actress (Ananya)", "year", "2025", "format", "Malayalam Theatrical Feature"),
                Map.of("title", "Echoes of the Stage", "role", "Lead Protagonist", "year", "2024", "format", "National Theatre Tour")
        ));
        talentMap.put(t2.getId(), t2);

        // Talent 7: Arjun Varma (Acting - Lead Actor in Hyderabad)
        TalentDto t7 = new TalentDto();
        t7.setId("t-7");
        t7.setName("Arjun Varma");
        t7.setRole("Lead Actor");
        t7.setCategory("Acting");
        t7.setPrimaryCraft("Cast (Actors)");
        t7.setSubCraft("Lead Actor");
        t7.setLocation("Hyderabad, Telangana, India");
        t7.setExperience("7 Years");
        t7.setAvatar("https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=400&q=80");
        t7.setCoverImage("https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?auto=format&fit=crop&w=1200&q=80");
        t7.setBio("Dynamic lead actor specializing in intense romantic dramas, action thrillers, and commercial feature films in Telugu and Pan-India cinema.");
        t7.setSkills(List.of("Lead Actor", "Method Acting", "Action Stunts", "Telugu & Hindi Diction", "Horse Riding"));
        t7.setHeadline("Lead Actor | Commercial & Indie Features | Telugu & Pan-India Cinema");
        t7.setAvailability("Booking for 2026/2027 Schedules");
        t7.setVerified(true);
        t7.setConnectionsCount(780);
        t7.setFollowersCount(8900);
        talentMap.put(t7.getId(), t7);

        // Talent 8: Rohan Vats (Acting - Character Actor & Antagonist in Mumbai)
        TalentDto t8 = new TalentDto();
        t8.setId("t-8");
        t8.setName("Rohan Vats");
        t8.setRole("Character Actor & Antagonist");
        t8.setCategory("Acting");
        t8.setPrimaryCraft("Cast (Actors)");
        t8.setSubCraft("Character Actor");
        t8.setLocation("Mumbai, Maharashtra, India");
        t8.setExperience("10 Years");
        t8.setAvatar("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80");
        t8.setCoverImage("https://images.unsplash.com/photo-1460723237483-7a6dc9d0b212?auto=format&fit=crop&w=1200&q=80");
        t8.setBio("Versatile character actor and antagonist with extensive theatre and feature film background across Hindi and regional cinema.");
        t8.setSkills(List.of("Character Actor", "Antagonist / Villain", "Improvisation", "Voice Modulation", "Fight Choreography"));
        t8.setHeadline("Character Actor & Antagonist | Hindi & Regional Cinema");
        t8.setAvailability("Available Immediately");
        t8.setVerified(true);
        t8.setConnectionsCount(590);
        t8.setFollowersCount(4300);
        talentMap.put(t8.getId(), t8);

        // Talent 9: Pooja Krishnan (Acting - Supporting Actress in Chennai)
        TalentDto t9 = new TalentDto();
        t9.setId("t-9");
        t9.setName("Pooja Krishnan");
        t9.setRole("Supporting Actress & Dubbing Artist");
        t9.setCategory("Acting");
        t9.setPrimaryCraft("Cast (Actors)");
        t9.setSubCraft("Supporting Actress");
        t9.setLocation("Chennai, Tamil Nadu, India");
        t9.setExperience("4 Years");
        t9.setAvatar("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80");
        t9.setBio("Supporting actress and voice artist fluent in Tamil, Telugu, and Malayalam. Experienced in family dramas and streaming series.");
        t9.setSkills(List.of("Supporting Actress", "Voice Actor", "Dubbing", "Expressive Delivery", "Classical Dance"));
        t9.setHeadline("Supporting Actress & Voice Artist | South Indian Cinema");
        t9.setAvailability("Open for Auditions");
        t9.setVerified(true);
        t9.setConnectionsCount(340);
        t9.setFollowersCount(2100);
        talentMap.put(t9.getId(), t9);

        // Talent 10: Deva Raj (Acting - Comedian in Hyderabad)
        TalentDto t10 = new TalentDto();
        t10.setId("t-10");
        t10.setName("Deva Raj");
        t10.setRole("Comedian & Character Artist");
        t10.setCategory("Acting");
        t10.setPrimaryCraft("Cast (Actors)");
        t10.setSubCraft("Comedian");
        t10.setLocation("Hyderabad, Telangana, India");
        t10.setExperience("8 Years");
        t10.setAvatar("https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=400&q=80");
        t10.setBio("Comic performer and situational humor specialist with over 20 feature film credits in Telugu cinema.");
        t10.setSkills(List.of("Comedian", "Comic Timing", "Dialect Mimicry", "Character Artist"));
        t10.setHeadline("Comedian & Character Artist | 20+ Feature Film Credits");
        t10.setAvailability("Available from Nov 2026");
        t10.setVerified(true);
        t10.setConnectionsCount(480);
        t10.setFollowersCount(3500);
        talentMap.put(t10.getId(), t10);

        // Talent 3: Vikramaditya Roy (Direction)
        TalentDto t3 = new TalentDto();
        t3.setId("t-3");
        t3.setName("Vikramaditya Roy");
        t3.setRole("Feature Film Director & Screenwriter");
        t3.setCategory("Direction");
        t3.setPrimaryCraft("Director");
        t3.setSubCraft("Director");
        t3.setLocation("Hyderabad, India");
        t3.setExperience("12 Years");
        t3.setAvatar("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=400&q=80");
        t3.setCoverImage("https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?auto=format&fit=crop&w=1200&q=80");
        t3.setBio("Filmmaker with three commercially successful and critically lauded features across Telugu and Hindi cinema. Specialist in high-concept thrillers and grounded action.");
        t3.setSkills(List.of("Story Development", "Multi-Camera Direction", "Talent Casting", "Visual Storyboarding", "Post-Production Supervision"));
        t3.setAvailability("Booking for Q2 2027");
        t3.setVerified(true);
        t3.setConnectionsCount(890);
        t3.setFollowersCount(5600);
        t3.setHeadline("Director & Screenwriter | 3 Box-Office Successes | Telugu & Hindi Cinema");
        talentMap.put(t3.getId(), t3);

        // Talent 4: Sanjana Sen (Art Direction)
        TalentDto t4 = new TalentDto();
        t4.setId("t-4");
        t4.setName("Sanjana Sen");
        t4.setRole("Production Designer & Art Director");
        t4.setCategory("Art Direction");
        t4.setPrimaryCraft("Art Director");
        t4.setSubCraft("Production Designer");
        t4.setLocation("Kolkata & Mumbai, India");
        t4.setExperience("7 Years");
        t4.setAvatar("https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=400&q=80");
        t4.setCoverImage("https://images.unsplash.com/photo-1460723237483-7a6dc9d0b212?auto=format&fit=crop&w=1200&q=80");
        t4.setBio("Production designer with background in architecture from CEPT. Specializes in period sets, world-building, and intricate miniature craft.");
        t4.setSkills(List.of("Set Design", "Period Reconstruction", "3D SketchUp & Rhino", "Prop Fabrication", "Budget Optimization"));
        t4.setAvailability("Available from Dec 2026");
        t4.setVerified(true);
        t4.setConnectionsCount(330);
        t4.setFollowersCount(1420);
        talentMap.put(t4.getId(), t4);

        // Talent 5: Kabir Ahmed (Sound Design)
        TalentDto t5 = new TalentDto();
        t5.setId("t-5");
        t5.setName("Kabir Ahmed");
        t5.setRole("Sync Sound Recordist & Sound Designer");
        t5.setCategory("Sound Design");
        t5.setPrimaryCraft("Audiography (Sound Recording/Mixing)");
        t5.setSubCraft("Sound Designer");
        t5.setLocation("Chennai & Mumbai, India");
        t5.setExperience("9 Years");
        t5.setAvatar("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80");
        t5.setCoverImage("https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?auto=format&fit=crop&w=1200&q=80");
        t5.setBio("National Award nominee for Best Sound Mixing. Proficient with Sound Devices 888, ambisonic microphones, and Dolby Atmos mastering suites.");
        t5.setSkills(List.of("Dolby Atmos Mixing", "Location Sync Sound", "Foley Design", "Pro Tools HD", "Sound Devices 8-Series"));
        t5.setAvailability("Available Immediately");
        t5.setVerified(true);
        t5.setConnectionsCount(640);
        t5.setFollowersCount(2100);
        talentMap.put(t5.getId(), t5);

        // Talent 6: Ananya Roy (Editing)
        TalentDto t6 = new TalentDto();
        t6.setId("t-6");
        t6.setName("Ananya Roy");
        t6.setRole("Lead Film Editor & Post Supervisor");
        t6.setCategory("Editing");
        t6.setPrimaryCraft("Editor");
        t6.setSubCraft("Lead Film Editor");
        t6.setLocation("Mumbai, India");
        t6.setExperience("6 Years");
        t6.setAvatar("https://images.unsplash.com/photo-1580489944761-15a19d654956?auto=format&fit=crop&w=400&q=80");
        t6.setCoverImage("https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?auto=format&fit=crop&w=1200&q=80");
        t6.setBio("Feature and trailer editor with credits on premier OTT series and box office hits. Known for razor-sharp narrative pacing and rhythmic montage assembly.");
        t6.setSkills(List.of("Avid Media Composer", "Premiere Pro", "DaVinci Resolve", "Workflow Architecture", "DCP Mastering"));
        t6.setAvailability("Available from Jan 2027");
        t6.setVerified(true);
        t6.setConnectionsCount(470);
        t6.setFollowersCount(1790);
        talentMap.put(t6.getId(), t6);
    }
}
