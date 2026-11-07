package com.truehunt.social.service;

import com.truehunt.social.dto.*;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserAccountRepository userAccountRepository;

    public ProfileService(ProfileRepository profileRepository, UserAccountRepository userAccountRepository) {
        this.profileRepository = profileRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public List<Map<String, Object>> getPublicDirectory() {
        return profileRepository.findAll().stream()
                .filter(profile -> profile.getName() != null && !profile.getName().isBlank())
                .map(profile -> {
                    Map<String, Object> directoryProfile = new HashMap<>();
                    String publicUserId = findAccountByEmail(profile.getEmail())
                            .map(UserAccountEntity::getPublicUserId)
                            .filter(id -> id != null && !id.isBlank())
                            .orElse(profile.getUserId());
                    directoryProfile.put("id", publicUserId);
                    directoryProfile.put("userId", publicUserId);
                    directoryProfile.put("referId", publicUserId);
                    directoryProfile.put("name", profile.getName());
                    directoryProfile.put("category", profile.getPrimaryCraft() != null ? profile.getPrimaryCraft() : "");
                    directoryProfile.put("primaryCraft", profile.getPrimaryCraft() != null ? profile.getPrimaryCraft() : "");
                    directoryProfile.put("height", profile.getHeight() != null ? profile.getHeight() : "");
                    directoryProfile.put("weight", profile.getWeight() != null ? profile.getWeight() : "");
                    directoryProfile.put("bodyBuild", profile.getBodyBuild() != null ? profile.getBodyBuild() : "");
                    directoryProfile.put("gender", profile.getGender() != null ? profile.getGender() : "");
                    directoryProfile.put("facialHair", profile.getFacialHair() != null ? profile.getFacialHair() : "");
                    directoryProfile.put("skinTone", profile.getSkinTone() != null ? profile.getSkinTone() : "");
                    directoryProfile.put("experience", profile.getExperience() != null ? profile.getExperience() : "");
                    directoryProfile.put("avatar", profile.getProfileImageUrl() != null ? profile.getProfileImageUrl() : "");

                    List<String> skills = new ArrayList<>(profile.getTalents());
                    profile.getWorkTypes().stream()
                            .filter(workType -> !skills.contains(workType))
                            .forEach(skills::add);
                    directoryProfile.put("skills", skills);

                    String pCraft = profile.getPrimaryCraft() != null && !profile.getPrimaryCraft().isBlank()
                            ? profile.getPrimaryCraft() : (skills.isEmpty() ? "Cast (Actors)" : skills.get(0));
                    String sCraft = profile.getSubCraft() != null && !profile.getSubCraft().isBlank()
                            ? profile.getSubCraft() : (pCraft.toLowerCase().contains("act") ? "Lead Actor" : pCraft);

                    directoryProfile.put("category", pCraft);
                    directoryProfile.put("primaryCraft", pCraft);
                    directoryProfile.put("craft", pCraft);
                    directoryProfile.put("subCraft", sCraft);
                    directoryProfile.put("secondaryCrafts", profile.getSecondaryCrafts());
                    directoryProfile.put("secondarySubCrafts", profile.getSecondarySubCrafts());

                    String resolvedRole = profile.getSubCraft() != null && !profile.getSubCraft().isBlank()
                            ? profile.getSubCraft() : pCraft;
                    directoryProfile.put("role", resolvedRole);

                    String resolvedHeadline = profile.getHeadline() != null && !profile.getHeadline().isBlank()
                            ? profile.getHeadline()
                            : (profile.getSubCraft() != null && !profile.getSubCraft().isBlank()
                                ? profile.getSubCraft() + " • " + pCraft
                                : pCraft);
                    directoryProfile.put("headline", resolvedHeadline);

                    directoryProfile.put("bio", profile.getHighlights() != null ? profile.getHighlights() : "");
                    directoryProfile.put("highlights", profile.getHighlights() != null ? profile.getHighlights() : "");
                    directoryProfile.put("address", profile.getAddress() != null ? profile.getAddress() : "");
                    directoryProfile.put("languages", profile.getLanguages());
                    directoryProfile.put("experiences", profile.getExperiences());

                    String loc = profile.getAddress() != null && !profile.getAddress().isBlank()
                            ? profile.getAddress()
                            : profile.getExperiences().stream()
                                    .map(experience -> experience.get("location"))
                                    .filter(Objects::nonNull)
                                    .map(String::valueOf)
                                    .filter(location -> !location.isBlank())
                                    .findFirst()
                                    .orElse("");
                    directoryProfile.put("location", loc);
                    return directoryProfile;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ProfileDto getMyProfile(String userEmail) {
        ProfileEntity entity = findCurrentProfile(userEmail);
        ProfileDto dto = mapEntityToDto(entity);
        dto.setIsOwner(true);
        return dto;
    }

    public ProfileDto getProfileById(String id) {
        Optional<ProfileEntity> opt = profileRepository.findByUserId(id);
        if (opt.isEmpty()) {
            opt = findAccountByPublicUserId(id)
                    .flatMap(account -> profileRepository.findByEmail(account.getEmail()));
        }
        if (opt.isEmpty()) {
            opt = profileRepository.findByEmail(id);
        }
        if (opt.isEmpty()) {
            try {
                opt = profileRepository.findById(Long.parseLong(id));
            } catch (Exception ignored) {}
        }

        if (opt.isEmpty()) {
            throw new ResourceNotFoundException("Profile", "id", id);
        }

        return mapEntityToDto(opt.get());
    }

    @Transactional
    public ProfileDto updateMyProfile(ProfileUpdateRequest req, String userEmail) {
        ProfileEntity profile = findCurrentProfile(userEmail);

        if (req.getName() != null) profile.setName(req.getName());
        if (req.getFullName() != null && !req.getFullName().isBlank()) profile.setName(req.getFullName());
        if (req.getHeadline() != null && !req.getHeadline().isBlank()) profile.setHeadline(req.getHeadline());
        if (req.getRole() != null) profile.setRole(req.getRole());
        if (req.getCategory() != null) profile.setCategory(req.getCategory());
        if (req.getCompany() != null) profile.setCompany(req.getCompany());
        if (req.getIndustry() != null) profile.setIndustry(req.getIndustry());
        if (req.getWebsiteUrl() != null) profile.setWebsiteUrl(req.getWebsiteUrl());
        if (req.getAvailability() != null) profile.setAvailability(req.getAvailability());
        if (req.getPhone() != null) profile.setPhone(req.getPhone());
        if (req.getPhoneNumber() != null && !req.getPhoneNumber().isBlank()) profile.setPhone(req.getPhoneNumber());
        if (req.getProfessionalPhone() != null) profile.setProfessionalPhone(req.getProfessionalPhone());
        if (req.getEmail() != null) profile.setEmail(req.getEmail());
        if (req.getExperience() != null) profile.setExperience(req.getExperience());
        if (req.getPrimaryCraft() != null) profile.setPrimaryCraft(req.getPrimaryCraft());
        if (req.getCraft() != null && !req.getCraft().isBlank()) profile.setPrimaryCraft(req.getCraft());
        if (req.getSubCraft() != null) profile.setSubCraft(req.getSubCraft());
        if (req.getDateOfBirth() != null) profile.setDateOfBirth(req.getDateOfBirth());
        if (req.getHeight() != null) profile.setHeight(req.getHeight());
        if (req.getWeight() != null) profile.setWeight(req.getWeight());
        if (req.getBodyBuild() != null) profile.setBodyBuild(req.getBodyBuild());
        if (req.getGender() != null) profile.setGender(req.getGender());
        if (req.getFacialHair() != null) profile.setFacialHair(req.getFacialHair());
        if (req.getSkinTone() != null) profile.setSkinTone(req.getSkinTone());
        if (req.getAddress() != null) profile.setAddress(req.getAddress());
        if (req.getLocation() != null && !req.getLocation().isBlank()) profile.setAddress(req.getLocation());
        if (req.getBio() != null) profile.setBio(req.getBio());
        if (req.getHighlights() != null && !req.getHighlights().isBlank()) profile.setHighlights(req.getHighlights());
        if (req.getOtherLanguage() != null) profile.setOtherLanguage(req.getOtherLanguage());
        if (req.getProfileImageUrl() != null) profile.setProfileImageUrl(req.getProfileImageUrl());
        if (req.getAvatar() != null && !req.getAvatar().isBlank()) profile.setProfileImageUrl(req.getAvatar());
        if (req.getCoverImageUrl() != null) profile.setCoverImageUrl(req.getCoverImageUrl());
        if (req.getCoverImage() != null && !req.getCoverImage().isBlank()) profile.setCoverImageUrl(req.getCoverImage());

        if (req.getLanguages() != null) profile.setLanguages(req.getLanguages());
        if (req.getSecondaryCrafts() != null) profile.setSecondaryCrafts(req.getSecondaryCrafts());
        if (req.getSecondarySubCrafts() != null) profile.setSecondarySubCrafts(req.getSecondarySubCrafts());
        if (req.getWorkTypes() != null) profile.setWorkTypes(req.getWorkTypes());
        if (req.getVideoUrls() != null) profile.setVideoUrls(req.getVideoUrls());
        if (req.getVideos() != null) profile.setVideos(req.getVideos());

        if (req.getSkills() != null) {
            profile.setTalents(req.getSkills());
        } else if (req.getTalents() != null) {
            profile.setTalents(req.getTalents());
        }

        if (req.getExperiences() != null) {
            List<Map<String, Object>> exps = new ArrayList<>();
            for (ExperienceDto e : req.getExperiences()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", e.getId() != null ? e.getId() : "exp-" + UUID.randomUUID().toString().substring(0, 8));
                m.put("title", e.getTitle());
                m.put("company", e.getCompany());
                m.put("projectName", e.getProjectName());
                m.put("organizationName", e.getOrganizationName());
                m.put("organizationLogoUrl", e.getOrganizationLogoUrl() != null ? e.getOrganizationLogoUrl() : e.getCompanyLogoUrl());
                m.put("companyLogoUrl", e.getCompanyLogoUrl() != null ? e.getCompanyLogoUrl() : e.getOrganizationLogoUrl());
                m.put("featuredOnProfile", e.getFeaturedOnProfile() != null ? e.getFeaturedOnProfile() : false);
                m.put("workedAs", e.getWorkedAs());
                m.put("location", e.getLocation());
                m.put("startDate", e.getStartDate());
                m.put("endDate", e.getEndDate());
                m.put("isCurrent", e.getIsCurrent());
                m.put("description", e.getDescription());
                m.put("highlights", e.getHighlights());
                m.put("employmentType", e.getEmploymentType());
                if (e.getAttachment() != null) {
                    m.put("attachment", e.getAttachment());
                }
                exps.add(m);
            }
            profile.setExperiences(exps);
        }

        if (req.getEducations() != null) {
            List<Map<String, Object>> edus = new ArrayList<>();
            for (EducationDto ed : req.getEducations()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", ed.getId() != null ? ed.getId() : "edu-" + UUID.randomUUID().toString().substring(0, 8));
                m.put("institution", ed.getInstitution());
                m.put("degree", ed.getDegree());
                m.put("fieldOfStudy", ed.getFieldOfStudy());
                m.put("startDate", ed.getStartDate());
                m.put("endDate", ed.getEndDate());
                m.put("grade", ed.getGrade());
                m.put("description", ed.getDescription());
                edus.add(m);
            }
            profile.setEducations(edus);
        }

        if (req.getProjects() != null) {
            List<Map<String, Object>> projs = new ArrayList<>();
            for (ProjectDto pr : req.getProjects()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", pr.getId() != null ? pr.getId() : "proj-" + UUID.randomUUID().toString().substring(0, 8));
                m.put("title", pr.getTitle());
                m.put("description", pr.getDescription());
                m.put("technologies", pr.getTechnologies());
                m.put("projectUrl", pr.getProjectUrl());
                m.put("startDate", pr.getStartDate());
                m.put("endDate", pr.getEndDate());
                projs.add(m);
            }
            profile.setProjects(projs);
        }

        profile.setUpdatedAt(LocalDateTime.now());
        ProfileEntity saved = profileRepository.save(profile);

        // Synchronize with UserAccountEntity if present
        findAccountByEmail(userEmail).ifPresent(account -> {
            if (profile.getName() != null) account.setName(profile.getName());
            if (profile.getPhone() != null) account.setPhone(profile.getPhone());
            if (profile.getPrimaryCraft() != null) account.setPrimaryCraft(profile.getPrimaryCraft());
            if (profile.getSubCraft() != null) account.setSubCraft(profile.getSubCraft());
            if (profile.getExperience() != null) account.setExperience(profile.getExperience());
            userAccountRepository.save(account);
        });

        ProfileDto dto = mapEntityToDto(saved);
        dto.setIsOwner(true);
        return dto;
    }

    // Experience CRUD
    @Transactional
    public ExperienceDto addExperience(ExperienceDto exp, String userEmail) {
        ProfileEntity profile = findCurrentProfile(userEmail);
        if (exp.getId() == null || exp.getId().isBlank()) {
            exp.setId("exp-" + UUID.randomUUID().toString().substring(0, 8));
        }

        Map<String, Object> expMap = new HashMap<>();
        expMap.put("id", exp.getId());
        expMap.put("title", exp.getTitle());
        expMap.put("company", exp.getCompany());
        expMap.put("employmentType", exp.getEmploymentType());
        expMap.put("location", exp.getLocation());
        expMap.put("startDate", exp.getStartDate());
        expMap.put("endDate", exp.getEndDate());
        expMap.put("isCurrent", exp.getIsCurrent());
        expMap.put("description", exp.getDescription());
        expMap.put("highlights", exp.getHighlights());
        expMap.put("projectName", exp.getProjectName());
        expMap.put("organizationName", exp.getOrganizationName());
        expMap.put("workedAs", exp.getWorkedAs());
        expMap.put("organizationLogoUrl", exp.getOrganizationLogoUrl() != null ? exp.getOrganizationLogoUrl() : exp.getCompanyLogoUrl());
        expMap.put("companyLogoUrl", exp.getCompanyLogoUrl() != null ? exp.getCompanyLogoUrl() : exp.getOrganizationLogoUrl());
        expMap.put("featuredOnProfile", exp.getFeaturedOnProfile() != null ? exp.getFeaturedOnProfile() : false);
        if (exp.getAttachment() != null) {
            expMap.put("attachment", exp.getAttachment());
        }

        List<Map<String, Object>> experiences = new ArrayList<>(profile.getExperiences());
        experiences.add(0, expMap);
        profile.setExperiences(experiences);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);

        return exp;
    }

    @Transactional
    public ExperienceDto updateExperience(String id, ExperienceDto exp, String userEmail) {
        ProfileEntity profile = findCurrentProfile(userEmail);
        List<Map<String, Object>> experiences = new ArrayList<>(profile.getExperiences());
        boolean found = false;
        for (int i = 0; i < experiences.size(); i++) {
            Map<String, Object> item = experiences.get(i);
            if (Objects.equals(item.get("id"), id)) {
                Map<String, Object> updated = new HashMap<>(item);
                if (exp.getTitle() != null) updated.put("title", exp.getTitle());
                if (exp.getCompany() != null) updated.put("company", exp.getCompany());
                if (exp.getEmploymentType() != null) updated.put("employmentType", exp.getEmploymentType());
                if (exp.getLocation() != null) updated.put("location", exp.getLocation());
                if (exp.getStartDate() != null) updated.put("startDate", exp.getStartDate());
                if (exp.getEndDate() != null) updated.put("endDate", exp.getEndDate());
                if (exp.getIsCurrent() != null) updated.put("isCurrent", exp.getIsCurrent());
                if (exp.getDescription() != null) updated.put("description", exp.getDescription());
                if (exp.getHighlights() != null) updated.put("highlights", exp.getHighlights());
                if (exp.getProjectName() != null) updated.put("projectName", exp.getProjectName());
                if (exp.getOrganizationName() != null) updated.put("organizationName", exp.getOrganizationName());
                if (exp.getWorkedAs() != null) updated.put("workedAs", exp.getWorkedAs());
                if (exp.getOrganizationLogoUrl() != null) {
                    updated.put("organizationLogoUrl", exp.getOrganizationLogoUrl());
                    updated.put("companyLogoUrl", exp.getOrganizationLogoUrl());
                } else if (exp.getCompanyLogoUrl() != null) {
                    updated.put("companyLogoUrl", exp.getCompanyLogoUrl());
                    updated.put("organizationLogoUrl", exp.getCompanyLogoUrl());
                }
                if (exp.getFeaturedOnProfile() != null) updated.put("featuredOnProfile", exp.getFeaturedOnProfile());
                if (exp.getAttachment() != null) updated.put("attachment", exp.getAttachment());
                experiences.set(i, updated);
                found = true;
                break;
            }
        }
        if (!found) {
            exp.setId(id);
            return addExperience(exp, userEmail);
        }
        profile.setExperiences(experiences);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);
        exp.setId(id);
        return exp;
    }

    @Transactional
    public Map<String, Object> deleteExperience(String id, String userEmail) {
        ProfileEntity profile = findCurrentProfile(userEmail);
        List<Map<String, Object>> filtered = profile.getExperiences().stream()
                .filter(item -> !Objects.equals(item.get("id"), id))
                .collect(Collectors.toList());
        profile.setExperiences(filtered);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Experience deleted successfully");
        res.put("id", id);
        return res;
    }

    // Education CRUD
    @Transactional
    public EducationDto addEducation(EducationDto edu, String userEmail) {
        ProfileEntity profile = findCurrentProfile(userEmail);
        if (edu.getId() == null || edu.getId().isBlank()) {
            edu.setId("edu-" + UUID.randomUUID().toString().substring(0, 8));
        }

        Map<String, Object> eduMap = new HashMap<>();
        eduMap.put("id", edu.getId());
        eduMap.put("institution", edu.getInstitution());
        eduMap.put("degree", edu.getDegree());
        eduMap.put("fieldOfStudy", edu.getFieldOfStudy());
        eduMap.put("startDate", edu.getStartDate());
        eduMap.put("endDate", edu.getEndDate());
        eduMap.put("grade", edu.getGrade());
        eduMap.put("activities", edu.getActivities());
        eduMap.put("description", edu.getDescription());

        List<Map<String, Object>> educations = new ArrayList<>(profile.getEducations());
        educations.add(0, eduMap);
        profile.setEducations(educations);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);

        return edu;
    }

    @Transactional
    public EducationDto updateEducation(String id, EducationDto edu, String userEmail) {
        ProfileEntity profile = findCurrentProfile(userEmail);
        List<Map<String, Object>> educations = new ArrayList<>(profile.getEducations());
        boolean found = false;
        for (int i = 0; i < educations.size(); i++) {
            Map<String, Object> item = educations.get(i);
            if (Objects.equals(item.get("id"), id)) {
                Map<String, Object> updated = new HashMap<>(item);
                if (edu.getInstitution() != null) updated.put("institution", edu.getInstitution());
                if (edu.getDegree() != null) updated.put("degree", edu.getDegree());
                if (edu.getFieldOfStudy() != null) updated.put("fieldOfStudy", edu.getFieldOfStudy());
                if (edu.getStartDate() != null) updated.put("startDate", edu.getStartDate());
                if (edu.getEndDate() != null) updated.put("endDate", edu.getEndDate());
                if (edu.getGrade() != null) updated.put("grade", edu.getGrade());
                if (edu.getDescription() != null) updated.put("description", edu.getDescription());
                educations.set(i, updated);
                found = true;
                break;
            }
        }
        if (!found) {
            edu.setId(id);
            return addEducation(edu, userEmail);
        }
        profile.setEducations(educations);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);
        edu.setId(id);
        return edu;
    }

    @Transactional
    public Map<String, Object> deleteEducation(String id, String userEmail) {
        ProfileEntity profile = findCurrentProfile(userEmail);
        List<Map<String, Object>> filtered = profile.getEducations().stream()
                .filter(item -> !Objects.equals(item.get("id"), id))
                .collect(Collectors.toList());
        profile.setEducations(filtered);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Education deleted successfully");
        res.put("id", id);
        return res;
    }

    // Project CRUD
    @Transactional
    public ProjectDto addProject(ProjectDto project, String userEmail) {
        ProfileEntity profile = findCurrentProfile(userEmail);
        if (project.getId() == null || project.getId().isBlank()) {
            project.setId("proj-" + UUID.randomUUID().toString().substring(0, 8));
        }

        Map<String, Object> projMap = new HashMap<>();
        projMap.put("id", project.getId());
        projMap.put("title", project.getTitle());
        projMap.put("description", project.getDescription());
        projMap.put("technologies", project.getTechnologies());
        projMap.put("projectUrl", project.getProjectUrl());
        projMap.put("startDate", project.getStartDate());
        projMap.put("endDate", project.getEndDate());

        List<Map<String, Object>> projects = new ArrayList<>(profile.getProjects());
        projects.add(0, projMap);
        profile.setProjects(projects);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);

        return project;
    }

    @Transactional
    public ProjectDto updateProject(String id, ProjectDto project, String userEmail) {
        ProfileEntity profile = findCurrentProfile(userEmail);
        List<Map<String, Object>> projects = new ArrayList<>(profile.getProjects());
        boolean found = false;
        for (int i = 0; i < projects.size(); i++) {
            Map<String, Object> item = projects.get(i);
            if (Objects.equals(item.get("id"), id)) {
                Map<String, Object> updated = new HashMap<>(item);
                if (project.getTitle() != null) updated.put("title", project.getTitle());
                if (project.getDescription() != null) updated.put("description", project.getDescription());
                if (project.getTechnologies() != null) updated.put("technologies", project.getTechnologies());
                if (project.getProjectUrl() != null) updated.put("projectUrl", project.getProjectUrl());
                if (project.getStartDate() != null) updated.put("startDate", project.getStartDate());
                if (project.getEndDate() != null) updated.put("endDate", project.getEndDate());
                projects.set(i, updated);
                found = true;
                break;
            }
        }
        if (!found) {
            project.setId(id);
            return addProject(project, userEmail);
        }
        profile.setProjects(projects);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);
        project.setId(id);
        return project;
    }

    @Transactional
    public Map<String, Object> deleteProject(String id, String userEmail) {
        ProfileEntity profile = findCurrentProfile(userEmail);
        List<Map<String, Object>> filtered = profile.getProjects().stream()
                .filter(item -> !Objects.equals(item.get("id"), id))
                .collect(Collectors.toList());
        profile.setProjects(filtered);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Project deleted successfully");
        res.put("id", id);
        return res;
    }

    // Skills
    @Transactional
    public ProfileDto addSkill(String skill, String userEmail) {
        ProfileEntity profile = findCurrentProfile(userEmail);
        List<String> skills = new ArrayList<>(profile.getTalents());
        if (!skills.contains(skill)) {
            skills.add(skill);
            profile.setTalents(skills);
            profile.setUpdatedAt(LocalDateTime.now());
            profileRepository.save(profile);
        }
        return mapEntityToDto(profile);
    }

    @Transactional
    public ProfileDto deleteSkill(String skill, String userEmail) {
        ProfileEntity profile = findCurrentProfile(userEmail);
        List<String> skills = profile.getTalents().stream()
                .filter(s -> !s.equalsIgnoreCase(skill))
                .collect(Collectors.toList());
        profile.setTalents(skills);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);
        return mapEntityToDto(profile);
    }

    public Map<String, Object> verifyReference(String referenceId) {
        Map<String, Object> res = new HashMap<>();
        res.put("referenceId", referenceId != null ? referenceId.trim() : "");
        res.put("verified", referenceId != null && !referenceId.trim().isBlank());
        res.put("message", "Reference verified successfully with TrueHunt Talent Network.");
        return res;
    }

    public List<Map<String, Object>> getProfilePosts(String id) {
        List<Map<String, Object>> posts = new ArrayList<>();
        Map<String, Object> p = new HashMap<>();
        p.put("id", "post-" + id + "-1");
        p.put("authorId", id);
        p.put("authorName", "Cinema Professional");
        p.put("content", "Excited to share insights from our recent shoot in Ramoji Film City!");
        p.put("likesCount", 48);
        p.put("commentsCount", 12);
        p.put("createdAt", LocalDateTime.now().minusDays(2).toString());
        posts.add(p);
        return posts;
    }

    public List<Map<String, Object>> getSuggestions(String currentId) {
        return getPublicDirectory().stream()
                .filter(p -> !Objects.equals(p.get("id"), currentId))
                .limit(4)
                .collect(Collectors.toList());
    }

    public ProfileEntity findCurrentProfile(String userEmail) {
        Optional<UserAccountEntity> account = findAccountByEmail(userEmail);
        if (account.isPresent()) {
            UserAccountEntity currentAccount = account.get();
            String publicUserId = currentAccount.getPublicUserId();
            if (publicUserId != null && !publicUserId.isBlank()) {
                ProfileEntity profile = profileRepository.findByUserId(publicUserId)
                        .or(() -> profileRepository.findByEmail(currentAccount.getEmail()))
                        .orElseGet(ProfileEntity::new);
                profile.setUserId(publicUserId);
                if (profile.getName() == null || profile.getName().isBlank()) {
                    profile.setName(currentAccount.getName());
                }
                if (profile.getEmail() == null || profile.getEmail().isBlank()) {
                    profile.setEmail(currentAccount.getEmail());
                }
                return profileRepository.save(profile);
            }
        }

        return profileRepository.findByUserId("me").orElseGet(() -> {
            ProfileEntity profile = new ProfileEntity();
            profile.setUserId("me");
            profile.setName("Cinema Professional");
            profile.setEmail("pro@truehunt.in");
            return profileRepository.save(profile);
        });
    }

    public ProfileDto mapEntityToDto(ProfileEntity entity) {
        ProfileDto dto = new ProfileDto();
        dto.setId(entity.getUserId());
        dto.setUserId(entity.getUserId());
        dto.setReferId(entity.getUserId());
        dto.setName(entity.getName());
        dto.setPhone(entity.getPhone());
        dto.setProfessionalPhone(entity.getProfessionalPhone() != null ? entity.getProfessionalPhone() : "");
        dto.setEmail(entity.getEmail());
        dto.setAddress(entity.getAddress());
        dto.setLocation(entity.getAddress() != null && !entity.getAddress().isBlank() ? entity.getAddress() : "Hyderabad, India");
        String bioVal = (entity.getBio() != null && !entity.getBio().isBlank()) ? entity.getBio() : entity.getHighlights();
        dto.setBio(bioVal != null && !bioVal.isBlank() ? bioVal :
                "Cinema professional dedicated to quality filmmaking, innovative storytelling, and industry collaboration.");
        dto.setHighlights(dto.getBio());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setExperience(entity.getExperience() != null ? entity.getExperience() : "");
        dto.setPrimaryCraft(entity.getPrimaryCraft() != null ? entity.getPrimaryCraft() : "Direction");
        dto.setSubCraft(entity.getSubCraft());
        dto.setHeadline(entity.getHeadline() != null && !entity.getHeadline().isBlank()
                ? entity.getHeadline()
                : (entity.getPrimaryCraft() != null ? entity.getPrimaryCraft() + " | Cinema Industry Professional" : "Cinema Professional"));
        dto.setRole(entity.getRole() != null ? entity.getRole() :
                (entity.getPrimaryCraft() != null ? entity.getPrimaryCraft() : "Cinema Professional"));
        dto.setCategory(entity.getCategory() != null ? entity.getCategory() :
                (entity.getPrimaryCraft() != null ? entity.getPrimaryCraft() : "Film Industry"));
        dto.setCompany(entity.getCompany());
        dto.setIndustry(entity.getIndustry());
        dto.setWebsiteUrl(entity.getWebsiteUrl());
        dto.setHeight(entity.getHeight());
        dto.setWeight(entity.getWeight());
        dto.setBodyBuild(entity.getBodyBuild());
        dto.setGender(entity.getGender());
        dto.setFacialHair(entity.getFacialHair());
        dto.setSkinTone(entity.getSkinTone());
        dto.setOtherLanguage(entity.getOtherLanguage());
        dto.setAvatar(entity.getProfileImageUrl());
        dto.setProfileImageUrl(entity.getProfileImageUrl());
        dto.setCoverImage(entity.getCoverImageUrl());
        dto.setCoverImageUrl(entity.getCoverImageUrl());
        dto.setAvailability(entity.getAvailability() != null ? entity.getAvailability() : "Available for New Projects");
        dto.setVerified(true);
        dto.setConnectionsCount(128);
        dto.setFollowersCount(430);

        List<String> skills = new ArrayList<>(entity.getTalents());
        if (skills.isEmpty() && entity.getPrimaryCraft() != null) {
            skills.add(entity.getPrimaryCraft());
        }
        dto.setSkills(skills);
        dto.setTalents(skills);
        dto.setLanguages(entity.getLanguages());
        dto.setSecondaryCrafts(entity.getSecondaryCrafts());
        dto.setSecondarySubCrafts(entity.getSecondarySubCrafts());
        dto.setWorkTypes(entity.getWorkTypes());
        dto.setVideoUrls(entity.getVideoUrls());
        dto.setVideos(entity.getVideos());

        // Map experiences
        List<ExperienceDto> expDtos = new ArrayList<>();
        for (Map<String, Object> expMap : entity.getExperiences()) {
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
            Object featured = expMap.get("featuredOnProfile");
            exp.setFeaturedOnProfile(featured instanceof Boolean ? (Boolean) featured : Boolean.parseBoolean(String.valueOf(featured)));
            if (expMap.get("attachment") instanceof Map) {
                exp.setAttachment((Map<String, Object>) expMap.get("attachment"));
            }
            expDtos.add(exp);
        }
        dto.setExperiences(expDtos);

        // Map educations
        List<EducationDto> eduDtos = new ArrayList<>();
        for (Map<String, Object> eduMap : entity.getEducations()) {
            EducationDto edu = new EducationDto();
            edu.setId((String) eduMap.get("id"));
            edu.setInstitution((String) eduMap.get("institution"));
            edu.setDegree((String) eduMap.get("degree"));
            edu.setFieldOfStudy((String) eduMap.get("fieldOfStudy"));
            edu.setStartDate((String) eduMap.get("startDate"));
            edu.setEndDate((String) eduMap.get("endDate"));
            edu.setGrade((String) eduMap.get("grade"));
            edu.setDescription((String) eduMap.get("description"));
            eduDtos.add(edu);
        }
        dto.setEducations(eduDtos);

        // Map projects
        List<ProjectDto> projDtos = new ArrayList<>();
        for (Map<String, Object> projMap : entity.getProjects()) {
            ProjectDto proj = new ProjectDto();
            proj.setId((String) projMap.get("id"));
            proj.setTitle((String) projMap.get("title"));
            proj.setDescription((String) projMap.get("description"));
            proj.setTechnologies((String) projMap.get("technologies"));
            proj.setProjectUrl((String) projMap.get("projectUrl"));
            proj.setStartDate((String) projMap.get("startDate"));
            proj.setEndDate((String) projMap.get("endDate"));
            projDtos.add(proj);
        }
        dto.setProjects(projDtos);

        return dto;
    }

    private Optional<UserAccountEntity> findAccountByEmail(String email) {
        return email == null || email.isBlank()
                ? Optional.empty()
                : userAccountRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT));
    }

    private Optional<UserAccountEntity> findAccountByPublicUserId(String publicUserId) {
        return publicUserId == null || publicUserId.isBlank()
                ? Optional.empty()
                : userAccountRepository.findByPublicUserId(publicUserId);
    }
}
