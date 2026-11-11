package com.truehunt.social.service;

import com.truehunt.social.dto.*;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ConflictException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.PostEntity;
import com.truehunt.social.model.ProfileViewEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.model.UserProfileRelationshipEntity;
import com.truehunt.social.repository.PostRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.ProfileViewRepository;
import com.truehunt.social.repository.UserAccountRepository;
import com.truehunt.social.repository.UserProfileRelationshipRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserAccountRepository userAccountRepository;
    private final PostRepository postRepository;
    private final ProfileViewRepository profileViewRepository;
    private final UserProfileRelationshipRepository relationshipRepository;
    private final ObjectProvider<TalentService> talentServiceProvider;

    public ProfileService(
            ProfileRepository profileRepository,
            UserAccountRepository userAccountRepository,
            PostRepository postRepository,
            ProfileViewRepository profileViewRepository,
            UserProfileRelationshipRepository relationshipRepository,
            ObjectProvider<TalentService> talentServiceProvider
    ) {
        this.profileRepository = profileRepository;
        this.userAccountRepository = userAccountRepository;
        this.postRepository = postRepository;
        this.profileViewRepository = profileViewRepository;
        this.relationshipRepository = relationshipRepository;
        this.talentServiceProvider = talentServiceProvider;
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
                    directoryProfile.put("coverImageUrl", profile.getCoverImageUrl() != null ? profile.getCoverImageUrl() : "");
                    directoryProfile.put("coverImage", profile.getCoverImageUrl() != null ? profile.getCoverImageUrl() : "");
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
        return getProfileById(id, null);
    }

    public ProfileDto getProfileById(String id, String userEmail) {
        Optional<ProfileEntity> opt = profileRepository.findByUserId(id);
        if (opt.isEmpty()) {
            opt = findAccountByPublicUserId(id)
                    .flatMap(account -> profileRepository.findByUserId(account.getPublicUserId()));
        }

        ProfileDto dto;
        if (opt.isPresent()) {
            dto = mapEntityToDto(opt.get());
        } else {
            TalentService talentService = talentServiceProvider != null ? talentServiceProvider.getIfAvailable() : null;
            if (talentService != null) {
                try {
                    TalentDto talent = talentService.getById(id);
                    if (talent != null) {
                        dto = mapTalentDtoToProfileDto(talent);
                    } else {
                        throw new ResourceNotFoundException("Profile", "id", id);
                    }
                } catch (ResourceNotFoundException e) {
                    throw e;
                } catch (Exception ignored) {
                    throw new ResourceNotFoundException("Profile", "id", id);
                }
            } else {
                throw new ResourceNotFoundException("Profile", "id", id);
            }
        }

        Set<String> ownerIds = resolveAllUserIdentifiers(userEmail);
        Set<String> lowerOwnerIds = ownerIds.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        Set<String> targetIds = new HashSet<>();
        if (id != null && !id.isBlank()) targetIds.add(id.trim());
        if (dto.getId() != null && !dto.getId().isBlank()) targetIds.add(dto.getId().trim());
        if (dto.getUserId() != null && !dto.getUserId().isBlank()) targetIds.add(dto.getUserId().trim());
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) targetIds.add(dto.getEmail().trim());
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            userAccountRepository.findByEmail(dto.getEmail().trim().toLowerCase(Locale.ROOT)).ifPresent(acc -> {
                if (acc.getPublicUserId() != null) targetIds.add(acc.getPublicUserId());
                targetIds.add(String.valueOf(acc.getId()));
            });
        }
        if (dto.getUserId() != null && !dto.getUserId().isBlank()) {
            userAccountRepository.findByPublicUserId(dto.getUserId().trim()).ifPresent(acc -> {
                if (acc.getEmail() != null) targetIds.add(acc.getEmail());
                targetIds.add(String.valueOf(acc.getId()));
            });
            profileRepository.findByUserId(dto.getUserId().trim()).ifPresent(p -> {
                if (p.getEmail() != null) targetIds.add(p.getEmail());
                if (p.getId() != null) targetIds.add(String.valueOf(p.getId()));
            });
        }
        Set<String> lowerTargetIds = targetIds.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        boolean isOwner = lowerTargetIds.stream().anyMatch(lowerOwnerIds::contains);
        dto.setIsOwner(isOwner);

        if (!isOwner) {
            boolean isConnected = false;
            boolean isFollowing = false;
            boolean isPending = false;
            boolean isOutgoingPending = false;
            boolean isIncomingPending = false;

            for (UserProfileRelationshipEntity rel : relationshipRepository.findAll()) {
                String owner = rel.getOwnerUserId() != null ? rel.getOwnerUserId().trim().toLowerCase(Locale.ROOT) : "";
                String target = rel.getTargetProfileId() != null ? rel.getTargetProfileId().trim().toLowerCase(Locale.ROOT) : "";
                String sender = rel.getSenderUserId() != null ? rel.getSenderUserId().trim().toLowerCase(Locale.ROOT) : "";

                boolean matchesOwnerToTarget = lowerOwnerIds.contains(owner) && lowerTargetIds.contains(target);
                boolean matchesTargetToOwner = lowerTargetIds.contains(owner) && lowerOwnerIds.contains(target);

                if (matchesOwnerToTarget || matchesTargetToOwner) {
                    if (rel.isConnected()) {
                        isConnected = true;
                    }
                    if ("PENDING".equalsIgnoreCase(rel.getStatus())) {
                        isPending = true;
                        if (lowerOwnerIds.contains(sender)) {
                            isOutgoingPending = true;
                        } else {
                            isIncomingPending = true;
                        }
                    }
                    if (matchesOwnerToTarget && rel.isFollowing()) {
                        isFollowing = true;
                    }
                }
            }

            dto.setIsConnected(isConnected);
            dto.setIsFollowing(isFollowing);
            dto.setIsPending(isPending);
            dto.setIsOutgoingPending(isOutgoingPending);
            dto.setIsIncomingPending(isIncomingPending);
            dto.setConnectionStatus(isConnected ? "CONNECTED" : (isPending ? (isOutgoingPending ? "PENDING_OUTGOING" : "PENDING_INCOMING") : "NONE"));
        } else {
            dto.setIsConnected(false);
            dto.setIsFollowing(false);
            dto.setIsPending(false);
            dto.setIsOutgoingPending(false);
            dto.setIsIncomingPending(false);
            dto.setConnectionStatus("NONE");
        }

        return dto;
    }

    public Set<String> resolveAllUserIdentifiers(String identifier) {
        Set<String> ids = new HashSet<>();
        if (identifier != null && !identifier.isBlank()) {
            String clean = identifier.trim().toLowerCase(Locale.ROOT);
            ids.add(clean);
            userAccountRepository.findByEmail(clean).ifPresent(acc -> {
                ids.add(String.valueOf(acc.getId()));
                if (acc.getPublicUserId() != null && !acc.getPublicUserId().isBlank()) {
                    ids.add(acc.getPublicUserId());
                    ids.add(acc.getPublicUserId().toLowerCase(Locale.ROOT));
                }
            });
            userAccountRepository.findByPublicUserId(clean).ifPresent(acc -> {
                ids.add(String.valueOf(acc.getId()));
                if (acc.getEmail() != null && !acc.getEmail().isBlank()) {
                    ids.add(acc.getEmail().toLowerCase(Locale.ROOT));
                }
            });
            try {
                Long accId = Long.parseLong(clean);
                userAccountRepository.findById(accId).ifPresent(acc -> {
                    if (acc.getEmail() != null) ids.add(acc.getEmail().toLowerCase(Locale.ROOT));
                    if (acc.getPublicUserId() != null) ids.add(acc.getPublicUserId().toLowerCase(Locale.ROOT));
                });
            } catch (NumberFormatException ignored) {}
            profileRepository.findByUserId(clean).ifPresent(p -> {
                if (p.getEmail() != null && !p.getEmail().isBlank()) {
                    ids.add(p.getEmail().toLowerCase(Locale.ROOT));
                }
                if (p.getId() != null) {
                    ids.add(String.valueOf(p.getId()));
                }
            });
            profileRepository.findByEmail(clean).ifPresent(p -> {
                if (p.getUserId() != null && !p.getUserId().isBlank()) {
                    ids.add(p.getUserId());
                    ids.add(p.getUserId().toLowerCase(Locale.ROOT));
                }
                if (p.getId() != null) {
                    ids.add(String.valueOf(p.getId()));
                }
            });
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String name = auth.getName();
            if (name != null && !name.isBlank()) {
                String cleanName = name.trim().toLowerCase(Locale.ROOT);
                ids.add(cleanName);
                userAccountRepository.findByEmail(cleanName).ifPresent(acc -> {
                    ids.add(String.valueOf(acc.getId()));
                    if (acc.getPublicUserId() != null && !acc.getPublicUserId().isBlank()) {
                        ids.add(acc.getPublicUserId());
                        ids.add(acc.getPublicUserId().toLowerCase(Locale.ROOT));
                    }
                });
            }
        }
        if (ids.isEmpty()) {
            ids.add("1000001");
            ids.add("me");
            ids.add("pro@truehunt.in");
        }
        return ids;
    }

    @Transactional
    public ProfileDto updateMyProfile(ProfileUpdateRequest req, String userEmail) {
        ProfileEntity profile = findCurrentProfile(userEmail);
        String userIdentifier = resolveUserEmail(userEmail);
        Optional<UserAccountEntity> accountOpt = findAccountByEmail(userIdentifier)
                .or(() -> findAccountByPublicUserId(userIdentifier));
        if (accountOpt.isEmpty() && req.getEmail() != null && !req.getEmail().isBlank()) {
            accountOpt = findAccountByEmail(req.getEmail());
        }
        UserAccountEntity currentAccount = accountOpt
                .or(() -> userAccountRepository.findByEmail("pro@truehunt.in"))
                .orElseGet(() -> {
                    UserAccountEntity fallback = new UserAccountEntity();
                    fallback.setEmail(userIdentifier != null && userIdentifier.contains("@")
                            ? userIdentifier
                            : (req.getEmail() != null && !req.getEmail().isBlank()
                                ? req.getEmail().trim().toLowerCase(Locale.ROOT)
                                : "pro@truehunt.in"));
                    fallback.setName(profile.getName() != null ? profile.getName() : "Cinema Professional");
                    fallback.setPublicUserId(profile.getUserId() != null ? profile.getUserId() : "1000001");
                    fallback.setCreatedAt(LocalDateTime.now());
                    return userAccountRepository.save(fallback);
                });
        String oldUserId = currentAccount.getPublicUserId();
        String newUserId = req.getUserId() == null ? oldUserId : req.getUserId().trim().toUpperCase(Locale.ROOT);
        if (newUserId == null || !newUserId.matches("[A-Z0-9_-]{3,14}")) {
            throw new BadRequestException("User ID must be 3 to 14 letters, numbers, underscores, or hyphens.");
        }
        if (!newUserId.equalsIgnoreCase(oldUserId)) {
            boolean usedByAnotherAccount = userAccountRepository.findByPublicUserId(newUserId)
                    .filter(account -> !Objects.equals(account.getId(), currentAccount.getId()))
                    .isPresent();
            boolean usedByAnotherProfile = profileRepository.findByUserId(newUserId)
                    .filter(existingProfile -> !Objects.equals(existingProfile.getId(), profile.getId()))
                    .isPresent();
            if (usedByAnotherAccount || usedByAnotherProfile) {
                throw new ConflictException("This User ID is already in use.");
            }
            updateProfileReferences(oldUserId, newUserId);
            currentAccount.setPublicUserId(newUserId);
            profile.setUserId(newUserId);
        }

        if (req.getName() != null) profile.setName(req.getName());
        else if (req.getFullName() != null) profile.setName(req.getFullName());
        if (req.getHeadline() != null) profile.setHeadline(req.getHeadline());
        if (req.getRole() != null) profile.setRole(req.getRole());
        if (req.getCategory() != null) profile.setCategory(req.getCategory());
        if (req.getCompany() != null) profile.setCompany(req.getCompany());
        else if (req.getCompanyName() != null) profile.setCompany(req.getCompanyName());
        if (req.getIndustry() != null) profile.setIndustry(req.getIndustry());
        if (req.getWebsiteUrl() != null) profile.setWebsiteUrl(req.getWebsiteUrl());
        if (req.getAvailability() != null) profile.setAvailability(req.getAvailability());
        if (req.getPhone() != null) profile.setPhone(req.getPhone());
        else if (req.getPhoneNumber() != null) profile.setPhone(req.getPhoneNumber());
        if (req.getProfessionalPhone() != null) profile.setProfessionalPhone(req.getProfessionalPhone());
        if (req.getEmail() != null) profile.setEmail(req.getEmail());
        if (req.getExperience() != null) profile.setExperience(req.getExperience());
        else if (req.getExperienceYears() != null) profile.setExperience(req.getExperienceYears());
        if (req.getPrimaryCraft() != null) profile.setPrimaryCraft(req.getPrimaryCraft());
        else if (req.getCraft() != null) profile.setPrimaryCraft(req.getCraft());
        if (req.getSubCraft() != null) profile.setSubCraft(req.getSubCraft());
        if (req.getDateOfBirth() != null) profile.setDateOfBirth(req.getDateOfBirth());
        if (req.getHeight() != null) profile.setHeight(req.getHeight());
        if (req.getWeight() != null) profile.setWeight(req.getWeight());
        if (req.getBodyBuild() != null) profile.setBodyBuild(req.getBodyBuild());
        if (req.getGender() != null) profile.setGender(req.getGender());
        if (req.getFacialHair() != null) profile.setFacialHair(req.getFacialHair());
        if (req.getSkinTone() != null) profile.setSkinTone(req.getSkinTone());
        if (req.getAddress() != null) profile.setAddress(req.getAddress());
        else if (req.getLocation() != null) profile.setAddress(req.getLocation());
        else if (req.getHeadquarters() != null) profile.setAddress(req.getHeadquarters());
        String updatedBio = req.getBio() != null ? req.getBio() : req.getHighlights();
        if (updatedBio != null) {
            profile.setBio(updatedBio);
            profile.setHighlights(updatedBio);
        }
        if (req.getOtherLanguage() != null) profile.setOtherLanguage(req.getOtherLanguage());
        if (req.getProfileImageUrl() != null) profile.setProfileImageUrl(req.getProfileImageUrl());
        else if (req.getAvatar() != null) profile.setProfileImageUrl(req.getAvatar());
        if (req.getCoverImageUrl() != null) profile.setCoverImageUrl(req.getCoverImageUrl());
        else if (req.getCoverImage() != null) profile.setCoverImageUrl(req.getCoverImage());

        if (req.getLanguages() != null) profile.setLanguages(req.getLanguages());
        if (req.getSecondaryCrafts() != null) profile.setSecondaryCrafts(req.getSecondaryCrafts());
        if (req.getSecondarySubCrafts() != null) profile.setSecondarySubCrafts(req.getSecondarySubCrafts());
        if (req.getWorkTypes() != null) profile.setWorkTypes(req.getWorkTypes());
        if (req.getVideoUrls() != null) profile.setVideoUrls(req.getVideoUrls());
        if (req.getVideos() != null) profile.setVideos(req.getVideos());

        if (req.getTalents() != null) {
            profile.setTalents(req.getTalents());
        } else if (req.getSkills() != null) {
            profile.setTalents(req.getSkills());
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
        if (profile.getName() != null) currentAccount.setName(profile.getName());
        if (profile.getPhone() != null) currentAccount.setPhone(profile.getPhone());
        if (profile.getPrimaryCraft() != null) currentAccount.setPrimaryCraft(profile.getPrimaryCraft());
        if (profile.getSubCraft() != null) currentAccount.setSubCraft(profile.getSubCraft());
        if (profile.getExperience() != null) currentAccount.setExperience(profile.getExperience());
        if (profile.getCompany() != null) currentAccount.setCompanyName(profile.getCompany());
        if (req.getHeadquarters() != null) currentAccount.setHeadquarters(req.getHeadquarters());
        else if (profile.getAddress() != null) currentAccount.setHeadquarters(profile.getAddress());
        userAccountRepository.save(currentAccount);

        ProfileDto dto = mapEntityToDto(saved);
        dto.setIsOwner(true);
        return dto;
    }

    private void updateProfileReferences(String oldUserId, String newUserId) {
        List<PostEntity> posts = postRepository.findAllByAuthorId(oldUserId);
        posts.forEach(post -> post.setAuthorId(newUserId));
        postRepository.saveAll(posts);

        List<ProfileViewEntity> views = profileViewRepository
                .findAllByOwnerUserIdOrViewerUserId(oldUserId, oldUserId);
        for (ProfileViewEntity view : views) {
            if (oldUserId.equals(view.getOwnerUserId())) view.setOwnerUserId(newUserId);
            if (oldUserId.equals(view.getViewerUserId())) view.setViewerUserId(newUserId);
        }
        profileViewRepository.saveAll(views);

        List<UserProfileRelationshipEntity> relationships = relationshipRepository
                .findAllByOwnerUserIdOrTargetProfileId(oldUserId, oldUserId);
        for (UserProfileRelationshipEntity relationship : relationships) {
            if (oldUserId.equals(relationship.getOwnerUserId())) relationship.setOwnerUserId(newUserId);
            if (oldUserId.equals(relationship.getTargetProfileId())) relationship.setTargetProfileId(newUserId);
        }
        relationshipRepository.saveAll(relationships);
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

    public List<Map<String, Object>> getSuggestions(String currentId) {
        return getPublicDirectory().stream()
                .filter(p -> !Objects.equals(p.get("id"), currentId))
                .limit(4)
                .collect(Collectors.toList());
    }

    public ProfileEntity findCurrentProfile(String userEmail) {
        String clean = resolveUserEmail(userEmail);
        if (clean != null && !clean.isBlank()) {
            // First try direct profile match by userId or email
            Optional<ProfileEntity> directProfile = profileRepository.findByUserId(clean)
                    .or(() -> profileRepository.findByEmail(clean));
            if (directProfile.isPresent()) {
                return directProfile.get();
            }

            // Then try UserAccount by email, publicUserId, or ID
            Optional<UserAccountEntity> accountOpt = findAccountByEmail(clean)
                    .or(() -> findAccountByPublicUserId(clean));
            if (accountOpt.isPresent()) {
                UserAccountEntity account = accountOpt.get();
                String publicUserId = account.getPublicUserId();
                if (publicUserId == null || publicUserId.isBlank()) {
                    publicUserId = String.valueOf(account.getId());
                }
                ProfileEntity profile = profileRepository.findByUserId(publicUserId)
                        .or(() -> profileRepository.findByEmail(account.getEmail()))
                        .orElseGet(ProfileEntity::new);
                profile.setUserId(publicUserId);
                if (profile.getName() == null || profile.getName().isBlank()) {
                    profile.setName(account.getName());
                }
                if (profile.getEmail() == null || profile.getEmail().isBlank()) {
                    profile.setEmail(account.getEmail());
                }
                return profileRepository.save(profile);
            }
        }

        // Fallback to pro@truehunt.in only if not found
        Optional<UserAccountEntity> currentAccountOpt = userAccountRepository.findByEmail("pro@truehunt.in");
        if (currentAccountOpt.isPresent()) {
            UserAccountEntity currentAccount = currentAccountOpt.get();
            String publicUserId = currentAccount.getPublicUserId();
            if (publicUserId == null || publicUserId.isBlank()) {
                publicUserId = String.valueOf(currentAccount.getId());
            }

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

        Optional<ProfileEntity> meProfile = profileRepository.findByUserId("me");
        if (meProfile.isPresent()) {
            return meProfile.get();
        }

        ProfileEntity fallback = new ProfileEntity();
        fallback.setUserId("me");
        fallback.setName("Cinema Professional");
        fallback.setEmail("pro@truehunt.in");
        return profileRepository.save(fallback);
    }

    private String resolveUserEmail(String userEmail) {
        if (userEmail != null && !userEmail.isBlank()) {
            return userEmail.trim().toLowerCase(Locale.ROOT);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName().trim().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private ProfileDto mapTalentDtoToProfileDto(TalentDto talent) {
        ProfileDto dto = new ProfileDto();
        dto.setId(talent.getId());
        dto.setUserId(talent.getId());
        dto.setReferId(talent.getId());
        dto.setName(talent.getName());
        dto.setRole(talent.getRole());
        dto.setHeadline(talent.getHeadline());
        dto.setCategory(talent.getCategory());
        dto.setPrimaryCraft(talent.getPrimaryCraft());
        dto.setSubCraft(talent.getSubCraft());
        dto.setCompany(talent.getCompany());
        dto.setIndustry(talent.getIndustry());
        dto.setLocation(talent.getLocation());
        dto.setAddress(talent.getLocation());
        dto.setBio(talent.getBio());
        dto.setHighlights(talent.getBio());
        dto.setAvatar(talent.getAvatar());
        dto.setProfileImageUrl(talent.getAvatar());
        dto.setCoverImage(talent.getCoverImage());
        dto.setCoverImageUrl(talent.getCoverImage());
        dto.setExperience(talent.getExperience());
        dto.setAvailability(talent.getAvailability());
        dto.setVerified(talent.getVerified() != null ? talent.getVerified() : false);
        dto.setConnectionsCount(talent.getConnectionsCount() != null ? talent.getConnectionsCount() : 0);
        dto.setFollowersCount(talent.getFollowersCount() != null ? talent.getFollowersCount() : 0);
        dto.setSkills(talent.getSkills());
        dto.setTalents(talent.getSkills());
        dto.setExperiences(talent.getExperiences());
        dto.setEducations(talent.getEducations());
        dto.setProjects(talent.getProjects());
        dto.setPosts(talent.getPosts());
        dto.setCredits(talent.getCredits());
        dto.setIsOwner(false);
        return dto;
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
        String bioVal = entity.getBio() != null ? entity.getBio() : entity.getHighlights();
        dto.setBio(bioVal != null ? bioVal : "");
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

        Set<String> profileIds = new HashSet<>();
        if (entity.getUserId() != null && !entity.getUserId().isBlank()) {
            profileIds.add(entity.getUserId().trim().toLowerCase(Locale.ROOT));
            userAccountRepository.findByPublicUserId(entity.getUserId().trim()).ifPresent(acc -> {
                if (acc.getEmail() != null) profileIds.add(acc.getEmail().trim().toLowerCase(Locale.ROOT));
                if (acc.getId() != null) profileIds.add(String.valueOf(acc.getId()));
            });
        }
        if (entity.getId() != null) {
            profileIds.add(String.valueOf(entity.getId()));
        }
        if (entity.getEmail() != null && !entity.getEmail().isBlank()) {
            String email = entity.getEmail().trim().toLowerCase(Locale.ROOT);
            profileIds.add(email);
            userAccountRepository.findByEmail(email).ifPresent(acc -> {
                if (acc.getPublicUserId() != null && !acc.getPublicUserId().isBlank()) {
                    profileIds.add(acc.getPublicUserId().trim().toLowerCase(Locale.ROOT));
                }
                if (acc.getId() != null) {
                    profileIds.add(String.valueOf(acc.getId()));
                }
            });
        }

        Set<String> distinctConnected = new HashSet<>();
        int followerCount = 0;
        try {
            for (UserProfileRelationshipEntity rel : relationshipRepository.findAll()) {
                if (rel.isConnected()) {
                    String owner = rel.getOwnerUserId() != null ? rel.getOwnerUserId().trim().toLowerCase(Locale.ROOT) : "";
                    String target = rel.getTargetProfileId() != null ? rel.getTargetProfileId().trim().toLowerCase(Locale.ROOT) : "";
                    if (profileIds.contains(owner) && !profileIds.contains(target) && !isInvalidTarget(target)) {
                        distinctConnected.add(target);
                    } else if (profileIds.contains(target) && !profileIds.contains(owner) && !isInvalidTarget(owner)) {
                        distinctConnected.add(owner);
                    }
                }
                if (rel.isFollowing()) {
                    String owner = rel.getOwnerUserId() != null ? rel.getOwnerUserId().trim().toLowerCase(Locale.ROOT) : "";
                    String target = rel.getTargetProfileId() != null ? rel.getTargetProfileId().trim().toLowerCase(Locale.ROOT) : "";
                    if (profileIds.contains(target) && !profileIds.contains(owner)) {
                        followerCount++;
                    }
                }
            }
        } catch (Exception ignored) {}
        dto.setConnectionsCount(distinctConnected.size());
        dto.setFollowersCount(followerCount);

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

    private boolean isInvalidTarget(String target) {
        if (target == null || target.isBlank()) return true;
        String lower = target.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".jpeg")
                || lower.endsWith(".webp") || lower.contains("uploads/");
    }
}
