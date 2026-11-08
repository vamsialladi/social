package com.truehunt.social.service;

import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.ProfileViewEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.ProfileViewRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ProfileViewService {

    private final ProfileRepository profileRepository;
    private final ProfileViewRepository profileViewRepository;
    private final UserAccountRepository userAccountRepository;

    public ProfileViewService(
            ProfileRepository profileRepository,
            ProfileViewRepository profileViewRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.profileRepository = profileRepository;
        this.profileViewRepository = profileViewRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public Map<String, Object> recordView(String profileId, String viewerEmail) {
        UserAccountEntity viewerAccount = requireAccount(viewerEmail);
        ProfileEntity owner = findProfile(profileId);
        String viewerUserId = viewerAccount.getPublicUserId();
        if (viewerUserId == null || viewerUserId.isBlank()) {
            throw new UnauthorizedException("The signed-in account does not have a profile identifier.");
        }
        String ownerUserId = canonicalUserId(owner);

        if (!ownerUserId.equalsIgnoreCase(viewerUserId)) {
            ProfileViewEntity view = profileViewRepository
                    .findByOwnerUserIdAndViewerUserId(ownerUserId, viewerUserId)
                    .orElseGet(ProfileViewEntity::new);
            view.setOwnerUserId(ownerUserId);
            view.setViewerUserId(viewerUserId);
            view.setViewedAt(LocalDateTime.now());
            profileViewRepository.save(view);
        }

        return Map.of("recorded", !ownerUserId.equalsIgnoreCase(viewerUserId));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyViewers(String ownerEmail) {
        UserAccountEntity ownerAccount = requireAccount(ownerEmail);
        String ownerUserId = ownerAccount.getPublicUserId();
        if (ownerUserId == null || ownerUserId.isBlank()) {
            throw new UnauthorizedException("The signed-in account does not have a profile identifier.");
        }

        return profileViewRepository.findAllByOwnerUserIdOrderByViewedAtDesc(ownerUserId).stream()
                .map(this::viewerSummary)
                .toList();
    }

    private UserAccountEntity requireAccount(String email) {
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("A signed-in account is required to access profile viewers.");
        }
        return userAccountRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UnauthorizedException("A signed-in account is required to access profile viewers."));
    }

    private ProfileEntity findProfile(String profileId) {
        return profileRepository.findByUserId(profileId)
                .or(() -> userAccountRepository.findByPublicUserId(profileId)
                        .flatMap(account -> profileRepository.findByUserId(account.getPublicUserId())))
                .or(() -> userAccountRepository.findByPublicUserId(profileId)
                        .flatMap(account -> profileRepository.findByEmail(account.getEmail())))
                .orElseThrow(() -> new ResourceNotFoundException("Profile", "id", profileId));
    }

    private String canonicalUserId(ProfileEntity profile) {
        return userAccountRepository.findByEmail(profile.getEmail() == null ? "" : profile.getEmail().trim().toLowerCase(Locale.ROOT))
                .map(UserAccountEntity::getPublicUserId)
                .filter(userId -> userId != null && !userId.isBlank())
                .orElse(profile.getUserId());
    }

    private Map<String, Object> viewerSummary(ProfileViewEntity view) {
        ProfileEntity profile = profileRepository.findByUserId(view.getViewerUserId()).orElse(null);
        UserAccountEntity account = userAccountRepository.findByPublicUserId(view.getViewerUserId()).orElse(null);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", view.getViewerUserId());
        summary.put("userId", view.getViewerUserId());
        summary.put("name", profile != null && profile.getName() != null
                ? profile.getName() : account != null ? account.getName() : "TrueHunt member");
        summary.put("headline", profile != null && profile.getHeadline() != null
                ? profile.getHeadline() : account != null ? account.getRole() : "");
        summary.put("profileImageUrl", profile != null && profile.getProfileImageUrl() != null
                ? profile.getProfileImageUrl() : "");
        summary.put("viewedAt", view.getViewedAt());
        return summary;
    }
}
