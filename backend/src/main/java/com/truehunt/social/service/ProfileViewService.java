package com.truehunt.social.service;

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
        if (viewerEmail == null || viewerEmail.isBlank()) {
            return Map.of("recorded", false);
        }
        UserAccountEntity viewerAccount = findAccountByIdentifier(viewerEmail);
        if (viewerAccount == null || "ADMIN".equalsIgnoreCase(viewerAccount.getRole())) {
            return Map.of("recorded", false);
        }
        ProfileEntity owner = profileRepository.findByUserId(profileId)
                .or(() -> userAccountRepository.findByPublicUserId(profileId)
                        .flatMap(account -> profileRepository.findByUserId(account.getPublicUserId())))
                .or(() -> userAccountRepository.findByPublicUserId(profileId)
                        .flatMap(account -> profileRepository.findByEmail(account.getEmail())))
                .orElse(null);
        if (owner == null) {
            return Map.of("recorded", false);
        }
        String viewerUserId = viewerAccount.getPublicUserId();
        if (viewerUserId == null || viewerUserId.isBlank()) {
            return Map.of("recorded", false);
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
        UserAccountEntity ownerAccount = findAccountByIdentifier(ownerEmail);
        if (ownerAccount == null || "ADMIN".equalsIgnoreCase(ownerAccount.getRole())) {
            return List.of();
        }
        String ownerUserId = ownerAccount.getPublicUserId();
        if (ownerUserId == null || ownerUserId.isBlank()) {
            return List.of();
        }

        return profileViewRepository.findAllByOwnerUserIdOrderByViewedAtDesc(ownerUserId).stream()
                .map(view -> {
                    UserAccountEntity viewer = userAccountRepository.findByPublicUserId(view.getViewerUserId())
                            .orElse(null);
                    return viewer != null && "ADMIN".equalsIgnoreCase(viewer.getRole())
                            ? null : viewerSummary(view, viewer);
                })
                .filter(summary -> summary != null)
                .toList();
    }

    private String canonicalUserId(ProfileEntity profile) {
        return userAccountRepository.findByEmail(profile.getEmail() == null ? "" : profile.getEmail().trim().toLowerCase(Locale.ROOT))
                .map(UserAccountEntity::getPublicUserId)
                .filter(userId -> userId != null && !userId.isBlank())
                .orElse(profile.getUserId());
    }

    private UserAccountEntity findAccountByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) return null;
        String normalized = identifier.trim();
        return userAccountRepository.findByEmail(normalized.toLowerCase(Locale.ROOT))
                .or(() -> userAccountRepository.findByPublicUserId(normalized))
                .orElse(null);
    }

    private Map<String, Object> viewerSummary(ProfileViewEntity view, UserAccountEntity account) {
        ProfileEntity profile = profileRepository.findByUserId(view.getViewerUserId())
                .or(() -> account == null
                        ? java.util.Optional.empty()
                        : profileRepository.findByEmail(account.getEmail()))
                .orElse(null);
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
