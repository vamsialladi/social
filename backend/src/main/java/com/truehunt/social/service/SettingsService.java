package com.truehunt.social.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truehunt.social.dto.PasswordChangeRequest;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.BlockedUserEntity;
import com.truehunt.social.model.NetworkProfileEntity;
import com.truehunt.social.model.PostEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.model.UserSettingsEntity;
import com.truehunt.social.repository.*;
import com.truehunt.social.validation.PasswordPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class SettingsService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern EXTERNAL_URL_PATTERN = Pattern.compile("(?i)(https?://\\S+|www\\.\\S+)");

    private static final Map<String, Object> DEFAULT_NOTIFICATION_PREFERENCES = createDefaultNotificationPreferences();
    private static final Map<String, Object> DEFAULT_PRIVACY_PREFERENCES = Map.of(
            "profileVisibility", true,
            "publicProfile", true,
            "public-profile", true,
            "profile_visibility", true,
            "activityStatus", true,
            "searchVisibility", true
    );
    private static final Map<String, Object> DEFAULT_RESTRICTED_PREFERENCES = Map.of(
            "sensitiveContent", true,
            "restrictedMessages", true
    );
    private static final Map<String, Object> DEFAULT_MEDIA_PREFERENCES = Map.of(
            "photoPosts", "public",
            "videoPosts", "public",
            "externalUrls", "public"
    );
    private static final Map<String, Object> DEFAULT_ENGAGEMENT_PREFERENCES = Map.of(
            "likeCounts", true,
            "shareCounts", true
    );

    private static Map<String, Object> createDefaultNotificationPreferences() {
        Map<String, Object> map = new LinkedHashMap<>();
        // Frontend Settings.jsx keys
        map.put("connections", true);
        map.put("connecting-with-others", true);
        map.put("network-catch-up", true);
        map.put("posting", true);
        map.put("posting-and-commenting", true);
        map.put("postingAndCommenting", true);
        map.put("messaging", true);
        map.put("news", true);
        map.put("profile", true);
        map.put("updating-your-profile", true);
        map.put("updatingYourProfile", true);
        map.put("verifications", true);
        // Legacy keys kept for backward compatibility
        map.put("emailNotifications", true);
        map.put("pushNotifications", true);
        map.put("opportunityAlerts", true);
        map.put("messageAlerts", true);
        return Collections.unmodifiableMap(map);
    }

    private final UserAccountRepository userAccountRepository;
    private final ProfileRepository profileRepository;
    private final NetworkProfileRepository networkProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSettingsRepository userSettingsRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final SavedPostRepository savedPostRepository;
    private final HiddenContentRepository hiddenContentRepository;
    private final PostRepository postRepository;

    // In-memory fallbacks used only when repositories are null (unit tests)
    private final Map<String, String> userThemes = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> userNotificationPreferences = new ConcurrentHashMap<>();

    public SettingsService(UserAccountRepository userAccountRepository,
                           ProfileRepository profileRepository,
                           NetworkProfileRepository networkProfileRepository,
                           PasswordEncoder passwordEncoder) {
        this(userAccountRepository, profileRepository, networkProfileRepository, passwordEncoder,
                null, null, null, null, null);
    }

    @Autowired
    public SettingsService(UserAccountRepository userAccountRepository,
                           ProfileRepository profileRepository,
                           NetworkProfileRepository networkProfileRepository,
                           PasswordEncoder passwordEncoder,
                           UserSettingsRepository userSettingsRepository,
                           BlockedUserRepository blockedUserRepository,
                           SavedPostRepository savedPostRepository,
                           HiddenContentRepository hiddenContentRepository,
                           PostRepository postRepository) {
        this.userAccountRepository = userAccountRepository;
        this.profileRepository = profileRepository;
        this.networkProfileRepository = networkProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.userSettingsRepository = userSettingsRepository;
        this.blockedUserRepository = blockedUserRepository;
        this.savedPostRepository = savedPostRepository;
        this.hiddenContentRepository = hiddenContentRepository;
        this.postRepository = postRepository;
    }

    private String resolveSettingsKey(String caller) {
        if (caller == null || caller.isBlank()) {
            return "default";
        }
        String trimmed = caller.trim();
        if ("me".equalsIgnoreCase(trimmed)) {
            if (userAccountRepository != null && userAccountRepository.findByEmail("pro@truehunt.in").isPresent()) {
                return "pro@truehunt.in";
            }
            return "me";
        }
        Optional<String> fromAccount = resolveUserAccount(trimmed)
                .map(UserAccountEntity::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .map(email -> email.trim().toLowerCase(Locale.ROOT));
        if (fromAccount.isPresent()) {
            return fromAccount.get();
        }
        if (profileRepository != null) {
            Optional<ProfileEntity> prof = profileRepository.findByUserId(trimmed)
                    .or(() -> profileRepository.findByUserId(trimmed.toUpperCase(Locale.ROOT)))
                    .or(() -> profileRepository.findByUserId(trimmed.toLowerCase(Locale.ROOT)))
                    .or(() -> profileRepository.findByEmail(trimmed.toLowerCase(Locale.ROOT)));
            if (prof.isPresent() && prof.get().getEmail() != null && !prof.get().getEmail().isBlank()) {
                return prof.get().getEmail().trim().toLowerCase(Locale.ROOT);
            }
        }
        if (trimmed.matches("^\\d+$")) {
            try {
                Long numericId = Long.parseLong(trimmed);
                if (userAccountRepository != null) {
                    Optional<UserAccountEntity> acc = userAccountRepository.findById(numericId);
                    if (acc.isPresent() && acc.get().getEmail() != null && !acc.get().getEmail().isBlank()) {
                        return acc.get().getEmail().trim().toLowerCase(Locale.ROOT);
                    }
                }
                if (profileRepository != null) {
                    Optional<ProfileEntity> prof = profileRepository.findById(numericId);
                    if (prof.isPresent() && prof.get().getEmail() != null && !prof.get().getEmail().isBlank()) {
                        return prof.get().getEmail().trim().toLowerCase(Locale.ROOT);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (networkProfileRepository != null) {
            Optional<NetworkProfileEntity> np = networkProfileRepository.findById(trimmed);
            if (np.isPresent() && np.get().getId() != null && !np.get().getId().isBlank()) {
                return np.get().getId().trim().toLowerCase(Locale.ROOT);
            }
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private UserSettingsEntity getOrCreateSettingsEntity(String key) {
        return userSettingsRepository.findByUserEmailIgnoreCase(key)
                .orElseGet(() -> {
                    UserSettingsEntity entity = new UserSettingsEntity(key);
                    entity.setTheme("dark");
                    entity.setNotificationsJson(toJson(DEFAULT_NOTIFICATION_PREFERENCES));
                    entity.setPrivacyJson(toJson(DEFAULT_PRIVACY_PREFERENCES));
                    entity.setRestrictedJson(toJson(DEFAULT_RESTRICTED_PREFERENCES));
                    entity.setMediaJson(toJson(DEFAULT_MEDIA_PREFERENCES));
                    entity.setEngagementJson(toJson(DEFAULT_ENGAGEMENT_PREFERENCES));
                    entity.setTwoFactorEnabled(false);
                    entity.setUpdatedAt(LocalDateTime.now());
                    return userSettingsRepository.save(entity);
                });
    }

    private Map<String, Object> getOrCreatePreferences(String key) {
        return userNotificationPreferences.computeIfAbsent(key, ignored -> new ConcurrentHashMap<>(DEFAULT_NOTIFICATION_PREFERENCES));
    }

    public Map<String, Object> getSettings() {
        return getSettings(null);
    }

    @Transactional
    public Map<String, Object> getSettings(String caller) {
        String key = resolveSettingsKey(caller);
        Map<String, Object> settings = new LinkedHashMap<>();

        if (userSettingsRepository != null) {
            UserSettingsEntity entity = getOrCreateSettingsEntity(key);
            settings.put("theme", entity.getTheme() != null && !entity.getTheme().isBlank() ? entity.getTheme() : "dark");
            settings.put("notifications", mergeWithDefaults(DEFAULT_NOTIFICATION_PREFERENCES, fromJsonMap(entity.getNotificationsJson())));
            settings.put("privacyPreferences", mergeWithDefaults(DEFAULT_PRIVACY_PREFERENCES, fromJsonMap(entity.getPrivacyJson())));
            settings.put("restrictedPreferences", mergeWithDefaults(DEFAULT_RESTRICTED_PREFERENCES, fromJsonMap(entity.getRestrictedJson())));
            settings.put("mediaPreferences", mergeWithDefaults(DEFAULT_MEDIA_PREFERENCES, fromJsonMap(entity.getMediaJson())));
            settings.put("engagementPreferences", mergeWithDefaults(DEFAULT_ENGAGEMENT_PREFERENCES, fromJsonMap(entity.getEngagementJson())));
            settings.put("twoFactorEnabled", entity.isTwoFactorEnabled());
        } else {
            settings.put("theme", userThemes.getOrDefault(key, "dark"));
            settings.put("notifications", new HashMap<>(getOrCreatePreferences(key)));
            settings.put("privacyPreferences", new HashMap<>(DEFAULT_PRIVACY_PREFERENCES));
            settings.put("restrictedPreferences", new HashMap<>(DEFAULT_RESTRICTED_PREFERENCES));
            settings.put("mediaPreferences", new HashMap<>(DEFAULT_MEDIA_PREFERENCES));
            settings.put("engagementPreferences", new HashMap<>(DEFAULT_ENGAGEMENT_PREFERENCES));
            settings.put("twoFactorEnabled", false);
        }

        settings.put("blockedUsers", getBlockedUsers(caller));
        settings.put("platformVersion", "2.4.0");
        return settings;
    }

    public Map<String, Object> updateTheme(String theme) {
        return updateTheme(theme, null);
    }

    @Transactional
    public Map<String, Object> updateTheme(String theme, String caller) {
        String key = resolveSettingsKey(caller);
        String resolvedTheme = (theme != null && !theme.isBlank()) ? theme.trim().toLowerCase(Locale.ROOT) : "dark";
        if (!"light".equals(resolvedTheme) && !"dark".equals(resolvedTheme)) {
            resolvedTheme = "dark";
        }

        if (userSettingsRepository != null) {
            UserSettingsEntity entity = getOrCreateSettingsEntity(key);
            entity.setTheme(resolvedTheme);
            entity.setUpdatedAt(LocalDateTime.now());
            userSettingsRepository.save(entity);
        } else {
            userThemes.put(key, resolvedTheme);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("theme", resolvedTheme);
        res.put("message", "Theme updated successfully");
        return res;
    }

    public Map<String, Object> updateNotificationPreferences(Map<String, Object> prefs) {
        return updateNotificationPreferences(prefs, null);
    }

    @Transactional
    public Map<String, Object> updateNotificationPreferences(Map<String, Object> prefs, String caller) {
        String key = resolveSettingsKey(caller);
        Map<String, Object> merged;
        if (userSettingsRepository != null) {
            UserSettingsEntity entity = getOrCreateSettingsEntity(key);
            merged = mergeWithDefaults(DEFAULT_NOTIFICATION_PREFERENCES, fromJsonMap(entity.getNotificationsJson()));
            if (prefs != null) {
                merged.putAll(prefs);
                if (prefs.containsKey("connections")) {
                    merged.put("connecting-with-others", prefs.get("connections"));
                } else if (prefs.containsKey("connecting-with-others")) {
                    merged.put("connections", prefs.get("connecting-with-others"));
                }
                if (prefs.containsKey("posting")) {
                    merged.put("posting-and-commenting", prefs.get("posting"));
                    merged.put("postingAndCommenting", prefs.get("posting"));
                } else if (prefs.containsKey("posting-and-commenting")) {
                    merged.put("posting", prefs.get("posting-and-commenting"));
                    merged.put("postingAndCommenting", prefs.get("posting-and-commenting"));
                } else if (prefs.containsKey("postingAndCommenting")) {
                    merged.put("posting", prefs.get("postingAndCommenting"));
                    merged.put("posting-and-commenting", prefs.get("postingAndCommenting"));
                }
                if (prefs.containsKey("profile")) {
                    merged.put("updating-your-profile", prefs.get("profile"));
                    merged.put("updatingYourProfile", prefs.get("profile"));
                } else if (prefs.containsKey("updating-your-profile")) {
                    merged.put("profile", prefs.get("updating-your-profile"));
                    merged.put("updatingYourProfile", prefs.get("updating-your-profile"));
                } else if (prefs.containsKey("updatingYourProfile")) {
                    merged.put("profile", prefs.get("updatingYourProfile"));
                    merged.put("updating-your-profile", prefs.get("updatingYourProfile"));
                }
            }
            entity.setNotificationsJson(toJson(merged));
            entity.setUpdatedAt(LocalDateTime.now());
            userSettingsRepository.save(entity);
        } else {
            Map<String, Object> userPrefs = getOrCreatePreferences(key);
            if (prefs != null) {
                userPrefs.putAll(prefs);
                if (prefs.containsKey("connections")) {
                    userPrefs.put("connecting-with-others", prefs.get("connections"));
                } else if (prefs.containsKey("connecting-with-others")) {
                    userPrefs.put("connections", prefs.get("connecting-with-others"));
                }
                if (prefs.containsKey("posting")) {
                    userPrefs.put("posting-and-commenting", prefs.get("posting"));
                    userPrefs.put("postingAndCommenting", prefs.get("posting"));
                } else if (prefs.containsKey("posting-and-commenting")) {
                    userPrefs.put("posting", prefs.get("posting-and-commenting"));
                    userPrefs.put("postingAndCommenting", prefs.get("posting-and-commenting"));
                } else if (prefs.containsKey("postingAndCommenting")) {
                    userPrefs.put("posting", prefs.get("postingAndCommenting"));
                    userPrefs.put("posting-and-commenting", prefs.get("postingAndCommenting"));
                }
                if (prefs.containsKey("profile")) {
                    userPrefs.put("updating-your-profile", prefs.get("profile"));
                    userPrefs.put("updatingYourProfile", prefs.get("profile"));
                } else if (prefs.containsKey("updating-your-profile")) {
                    userPrefs.put("profile", prefs.get("updating-your-profile"));
                    userPrefs.put("updatingYourProfile", prefs.get("updating-your-profile"));
                } else if (prefs.containsKey("updatingYourProfile")) {
                    userPrefs.put("profile", prefs.get("updatingYourProfile"));
                    userPrefs.put("updating-your-profile", prefs.get("updatingYourProfile"));
                }
            }
            merged = new HashMap<>(userPrefs);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("notifications", merged);
        res.put("message", "Preferences saved successfully");
        return res;
    }

    @Transactional
    public Map<String, Object> updatePrivacyPreferences(Map<String, Object> prefs, String caller) {
        String key = resolveSettingsKey(caller);
        Map<String, Object> merged;
        Boolean profileVisibilityVal = null;
        if (prefs != null) {
            for (String alias : List.of("profileVisibility", "publicProfile", "public-profile", "profile_visibility")) {
                if (prefs.containsKey(alias)) {
                    Object v = prefs.get(alias);
                    if (v instanceof Boolean b) {
                        profileVisibilityVal = b;
                        break;
                    } else if (v instanceof String s) {
                        profileVisibilityVal = !"false".equalsIgnoreCase(s.trim()) && !"off".equalsIgnoreCase(s.trim());
                        break;
                    }
                }
            }
        }
        if (userSettingsRepository != null) {
            UserSettingsEntity entity = getOrCreateSettingsEntity(key);
            merged = mergeWithDefaults(DEFAULT_PRIVACY_PREFERENCES, fromJsonMap(entity.getPrivacyJson()));
            if (prefs != null) {
                for (Map.Entry<String, Object> entry : prefs.entrySet()) {
                    if (DEFAULT_PRIVACY_PREFERENCES.containsKey(entry.getKey()) && entry.getValue() instanceof Boolean) {
                        merged.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            if (profileVisibilityVal != null) {
                merged.put("profileVisibility", profileVisibilityVal);
                merged.put("publicProfile", profileVisibilityVal);
                merged.put("public-profile", profileVisibilityVal);
                merged.put("profile_visibility", profileVisibilityVal);
            }
            entity.setPrivacyJson(toJson(merged));
            entity.setUpdatedAt(LocalDateTime.now());
            userSettingsRepository.save(entity);
        } else {
            merged = mergeWithDefaults(DEFAULT_PRIVACY_PREFERENCES, prefs);
            if (profileVisibilityVal != null) {
                merged.put("profileVisibility", profileVisibilityVal);
                merged.put("publicProfile", profileVisibilityVal);
                merged.put("public-profile", profileVisibilityVal);
                merged.put("profile_visibility", profileVisibilityVal);
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("privacyPreferences", merged);
        res.put("message", "Privacy settings updated successfully");
        return res;
    }

    @Transactional
    public Map<String, Object> updateRestrictedPreferences(Map<String, Object> prefs, String caller) {
        String key = resolveSettingsKey(caller);
        Map<String, Object> merged;
        if (userSettingsRepository != null) {
            UserSettingsEntity entity = getOrCreateSettingsEntity(key);
            merged = mergeWithDefaults(DEFAULT_RESTRICTED_PREFERENCES, fromJsonMap(entity.getRestrictedJson()));
            if (prefs != null) {
                for (Map.Entry<String, Object> entry : prefs.entrySet()) {
                    if (DEFAULT_RESTRICTED_PREFERENCES.containsKey(entry.getKey()) && entry.getValue() instanceof Boolean) {
                        merged.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            entity.setRestrictedJson(toJson(merged));
            entity.setUpdatedAt(LocalDateTime.now());
            userSettingsRepository.save(entity);
        } else {
            merged = mergeWithDefaults(DEFAULT_RESTRICTED_PREFERENCES, prefs);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("restrictedPreferences", merged);
        res.put("message", "Restricted content preferences updated successfully");
        return res;
    }

    @Transactional
    public Map<String, Object> updateMediaPreferences(Map<String, Object> prefs, String caller) {
        String key = resolveSettingsKey(caller);
        Map<String, Object> merged;
        if (userSettingsRepository != null) {
            UserSettingsEntity entity = getOrCreateSettingsEntity(key);
            merged = mergeWithDefaults(DEFAULT_MEDIA_PREFERENCES, fromJsonMap(entity.getMediaJson()));
            if (prefs != null) {
                for (Map.Entry<String, Object> entry : prefs.entrySet()) {
                    if (DEFAULT_MEDIA_PREFERENCES.containsKey(entry.getKey()) && entry.getValue() != null) {
                        String mode = String.valueOf(entry.getValue()).trim().toLowerCase(Locale.ROOT);
                        if ("public".equals(mode) || "private".equals(mode)) {
                            merged.put(entry.getKey(), mode);
                        }
                    }
                }
            }
            entity.setMediaJson(toJson(merged));
            entity.setUpdatedAt(LocalDateTime.now());
            userSettingsRepository.save(entity);
            syncAuthorPostsAudienceWithMediaPreferences(caller, key, merged, prefs);
        } else {
            merged = mergeWithDefaults(DEFAULT_MEDIA_PREFERENCES, prefs);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("mediaPreferences", merged);
        res.put("message", "Media visibility preferences updated successfully");
        return res;
    }

    private void syncAuthorPostsAudienceWithMediaPreferences(
            String caller,
            String resolvedEmailKey,
            Map<String, Object> mergedPrefs,
            Map<String, Object> changedPrefs
    ) {
        if (postRepository == null || changedPrefs == null || changedPrefs.isEmpty()) {
            return;
        }
        Set<String> authorIdentities = new HashSet<>();
        if (caller != null && !caller.isBlank()) {
            authorIdentities.add(caller.trim().toLowerCase(Locale.ROOT));
        }
        if (resolvedEmailKey != null && !resolvedEmailKey.isBlank()) {
            authorIdentities.add(resolvedEmailKey.trim().toLowerCase(Locale.ROOT));
        }
        resolveUserAccount(caller).ifPresent(acc -> {
            if (acc.getEmail() != null) authorIdentities.add(acc.getEmail().trim().toLowerCase(Locale.ROOT));
            if (acc.getPublicUserId() != null) authorIdentities.add(acc.getPublicUserId().trim().toLowerCase(Locale.ROOT));
        });
        if (profileRepository != null && resolvedEmailKey != null && !resolvedEmailKey.isBlank()) {
            profileRepository.findByEmail(resolvedEmailKey).ifPresent(prof -> {
                if (prof.getUserId() != null) authorIdentities.add(prof.getUserId().trim().toLowerCase(Locale.ROOT));
            });
        }
        if (authorIdentities.contains("1000001") || authorIdentities.contains("pro@truehunt.in")) {
            authorIdentities.add("me");
        }
        if (authorIdentities.isEmpty()) return;

        List<PostEntity> updatedPosts = new ArrayList<>();
        for (PostEntity post : postRepository.findAll()) {
            String postAuthor = post.getAuthorId() != null ? post.getAuthorId().trim().toLowerCase(Locale.ROOT) : "";
            if (postAuthor.isEmpty() || !authorIdentities.contains(postAuthor)) continue;

            boolean isVideo = isVideoMedia(post.getMedia(), post.getMediaUrl(), post.getMediaType());
            boolean isPhoto = isPhotoMedia(post.getMedia(), post.getMediaUrl(), post.getMediaType(), isVideo);
            boolean hasUrl = hasExternalUrl(post.getTitle(), post.getContent());

            String targetAudience = null;
            if (isPhoto && changedPrefs.containsKey("photoPosts")) {
                targetAudience = String.valueOf(mergedPrefs.get("photoPosts"));
            } else if (isVideo && changedPrefs.containsKey("videoPosts")) {
                targetAudience = String.valueOf(mergedPrefs.get("videoPosts"));
            } else if (hasUrl && changedPrefs.containsKey("externalUrls")) {
                targetAudience = String.valueOf(mergedPrefs.get("externalUrls"));
            }

            if ("private".equalsIgnoreCase(targetAudience) || "public".equalsIgnoreCase(targetAudience)) {
                String normalized = targetAudience.toLowerCase(Locale.ROOT);
                if (!normalized.equalsIgnoreCase(post.getAudience()) || post.isAudienceOverride()) {
                    post.setAudience(normalized);
                    post.setAudienceOverride(false);
                    updatedPosts.add(post);
                }
            }
        }
        if (!updatedPosts.isEmpty()) {
            postRepository.saveAll(updatedPosts);
        }
    }

    @Transactional
    public Map<String, Object> updateEngagementPreferences(Map<String, Object> prefs, String caller) {
        String key = resolveSettingsKey(caller);
        Map<String, Object> merged;
        if (userSettingsRepository != null) {
            UserSettingsEntity entity = getOrCreateSettingsEntity(key);
            merged = mergeWithDefaults(DEFAULT_ENGAGEMENT_PREFERENCES, fromJsonMap(entity.getEngagementJson()));
            if (prefs != null) {
                for (Map.Entry<String, Object> entry : prefs.entrySet()) {
                    if (DEFAULT_ENGAGEMENT_PREFERENCES.containsKey(entry.getKey()) && entry.getValue() instanceof Boolean) {
                        merged.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            entity.setEngagementJson(toJson(merged));
            entity.setUpdatedAt(LocalDateTime.now());
            userSettingsRepository.save(entity);
        } else {
            merged = mergeWithDefaults(DEFAULT_ENGAGEMENT_PREFERENCES, prefs);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("engagementPreferences", merged);
        res.put("message", "Engagement preferences updated successfully");
        return res;
    }

    @Transactional
    public Map<String, Object> updateSecurityPreferences(Map<String, Object> prefs, String caller) {
        String key = resolveSettingsKey(caller);
        boolean twoFactor = false;
        if (prefs != null && prefs.get("twoFactorEnabled") instanceof Boolean b) {
            twoFactor = b;
        }
        if (userSettingsRepository != null) {
            UserSettingsEntity entity = getOrCreateSettingsEntity(key);
            entity.setTwoFactorEnabled(twoFactor);
            entity.setUpdatedAt(LocalDateTime.now());
            userSettingsRepository.save(entity);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("twoFactorEnabled", twoFactor);
        res.put("message", twoFactor
                ? "Two-factor authentication enabled."
                : "Two-factor authentication turned off.");
        return res;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBlockedUsers(String caller) {
        if (blockedUserRepository == null) {
            return List.of();
        }
        String key = resolveSettingsKey(caller);
        return blockedUserRepository.findAllByBlockerEmailIgnoreCaseOrderByCreatedAtDesc(key).stream()
                .map(BlockedUserEntity::toDto)
                .toList();
    }

    @Transactional
    public Map<String, Object> blockUser(Map<String, Object> req, String caller) {
        if (caller == null || caller.isBlank()) {
            throw new UnauthorizedException("Sign in to block a user.");
        }
        if (req == null) {
            throw new BadRequestException("Blocked user details are required.");
        }
        String blockerKey = resolveSettingsKey(caller);
        String identity = firstNonBlank(req, "identity", "email", "userId", "id", "name").toLowerCase(Locale.ROOT);
        if (identity.isEmpty()) {
            throw new BadRequestException("User identity is required to block.");
        }
        String blockedEmail = firstNonBlank(req, "email").toLowerCase(Locale.ROOT);
        String blockedName = firstNonBlank(req, "name");
        String blockedPhoto = firstNonBlank(req, "photo", "profileImageUrl", "avatar");
        String reason = firstNonBlank(req, "reason");
        if (reason.isEmpty()) {
            reason = "Blocked by user";
        }

        if (blockedUserRepository == null) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("identity", identity);
            fallback.put("name", blockedName);
            fallback.put("email", blockedEmail);
            fallback.put("photo", blockedPhoto);
            fallback.put("reason", reason);
            return fallback;
        }

        BlockedUserEntity entity = blockedUserRepository
                .findByBlockerEmailIgnoreCaseAndBlockedIdentityIgnoreCase(blockerKey, identity)
                .orElseGet(BlockedUserEntity::new);
        entity.setBlockerEmail(blockerKey);
        entity.setBlockedIdentity(identity);
        entity.setBlockedEmail(blockedEmail);
        entity.setBlockedName(blockedName);
        entity.setBlockedPhoto(blockedPhoto);
        entity.setReason(reason);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        return blockedUserRepository.save(entity).toDto();
    }

    @Transactional
    public Map<String, Object> unblockUser(String identity, String caller) {
        if (caller == null || caller.isBlank()) {
            throw new UnauthorizedException("Sign in to unblock a user.");
        }
        if (identity == null || identity.isBlank()) {
            throw new BadRequestException("Blocked user identity is required.");
        }
        String blockerKey = resolveSettingsKey(caller);
        String normalizedIdentity = identity.trim().toLowerCase(Locale.ROOT);
        if (blockedUserRepository != null) {
            blockedUserRepository.findByBlockerEmailIgnoreCaseAndBlockedIdentityIgnoreCase(blockerKey, normalizedIdentity)
                    .ifPresent(blockedUserRepository::delete);
            blockedUserRepository.findByBlockerEmailIgnoreCaseAndBlockedEmailIgnoreCase(blockerKey, normalizedIdentity)
                    .ifPresent(blockedUserRepository::delete);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("identity", normalizedIdentity);
        res.put("unblocked", true);
        res.put("message", "User unblocked successfully.");
        return res;
    }

    @Transactional(readOnly = true)
    public Set<String> getBlockedIdentitySet(String caller) {
        if (blockedUserRepository == null || caller == null || caller.isBlank()) {
            return Set.of();
        }
        String blockerKey = resolveSettingsKey(caller);
        Set<String> set = new HashSet<>();
        for (BlockedUserEntity entry : blockedUserRepository.findAllByBlockerEmailIgnoreCaseOrderByCreatedAtDesc(blockerKey)) {
            if (entry.getBlockedIdentity() != null && !entry.getBlockedIdentity().isBlank()) {
                set.add(entry.getBlockedIdentity().trim().toLowerCase(Locale.ROOT));
            }
            if (entry.getBlockedEmail() != null && !entry.getBlockedEmail().isBlank()) {
                set.add(entry.getBlockedEmail().trim().toLowerCase(Locale.ROOT));
            }
            if (entry.getBlockedName() != null && !entry.getBlockedName().isBlank()) {
                set.add(entry.getBlockedName().trim().toLowerCase(Locale.ROOT));
            }
        }
        return set;
    }

    @Transactional(readOnly = true)
    public boolean isBlockedBetween(String userA, Set<String> userAIdentities, String userB, Set<String> userBIdentities) {
        if (blockedUserRepository == null) {
            return false;
        }
        Set<String> blockedByA = getBlockedIdentitySet(userA);
        if (userBIdentities != null) {
            for (String id : userBIdentities) {
                if (id != null && blockedByA.contains(id.trim().toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        Set<String> blockedByB = getBlockedIdentitySet(userB);
        if (userAIdentities != null) {
            for (String id : userAIdentities) {
                if (id != null && blockedByB.contains(id.trim().toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    private Optional<UserAccountEntity> resolveUserAccount(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String trimmed = identifier.trim();
        return userAccountRepository.findByEmail(trimmed.toLowerCase(Locale.ROOT))
                .or(() -> userAccountRepository.findByPublicUserId(trimmed))
                .or(() -> userAccountRepository.findByPublicUserId(trimmed.toUpperCase(Locale.ROOT)))
                .or(() -> userAccountRepository.findByPublicUserId(trimmed.toLowerCase(Locale.ROOT)));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMediaPreferencesForUser(String userIdentifier) {
        if (userSettingsRepository == null || userIdentifier == null || userIdentifier.isBlank()) {
            return new LinkedHashMap<>(DEFAULT_MEDIA_PREFERENCES);
        }
        String key = resolveSettingsKey(userIdentifier);
        return userSettingsRepository.findByUserEmailIgnoreCase(key)
                .map(entity -> mergeWithDefaults(DEFAULT_MEDIA_PREFERENCES, fromJsonMap(entity.getMediaJson())))
                .orElseGet(() -> new LinkedHashMap<>(DEFAULT_MEDIA_PREFERENCES));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEngagementPreferencesForUser(String userIdentifier) {
        if (userSettingsRepository == null || userIdentifier == null || userIdentifier.isBlank()) {
            return new LinkedHashMap<>(DEFAULT_ENGAGEMENT_PREFERENCES);
        }
        String key = resolveSettingsKey(userIdentifier);
        return userSettingsRepository.findByUserEmailIgnoreCase(key)
                .map(entity -> mergeWithDefaults(DEFAULT_ENGAGEMENT_PREFERENCES, fromJsonMap(entity.getEngagementJson())))
                .orElseGet(() -> new LinkedHashMap<>(DEFAULT_ENGAGEMENT_PREFERENCES));
    }

    @Transactional(readOnly = true)
    public boolean isLikeCountEnabledForAuthor(String authorIdentifier) {
        if (authorIdentifier == null || authorIdentifier.isBlank()) {
            return true;
        }
        Object val = getEngagementPreferencesForUser(authorIdentifier).get("likeCounts");
        return !Boolean.FALSE.equals(val);
    }

    @Transactional(readOnly = true)
    public boolean isShareCountEnabledForAuthor(String authorIdentifier) {
        if (authorIdentifier == null || authorIdentifier.isBlank()) {
            return true;
        }
        Object val = getEngagementPreferencesForUser(authorIdentifier).get("shareCounts");
        return !Boolean.FALSE.equals(val);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationPreferencesForUser(String userIdentifier) {
        if (userSettingsRepository == null || userIdentifier == null || userIdentifier.isBlank()) {
            return new LinkedHashMap<>(DEFAULT_NOTIFICATION_PREFERENCES);
        }
        String key = resolveSettingsKey(userIdentifier);
        return userSettingsRepository.findByUserEmailIgnoreCase(key)
                .map(entity -> mergeWithDefaults(DEFAULT_NOTIFICATION_PREFERENCES, fromJsonMap(entity.getNotificationsJson())))
                .orElseGet(() -> new LinkedHashMap<>(DEFAULT_NOTIFICATION_PREFERENCES));
    }

    @Transactional(readOnly = true)
    public boolean isConnectingWithOthersEnabled(String userIdentifier) {
        if (userIdentifier == null || userIdentifier.isBlank()) {
            return true;
        }
        Map<String, Object> prefs = getNotificationPreferencesForUser(userIdentifier);
        Object val = prefs.get("connections");
        if (val == null) {
            val = prefs.get("connecting-with-others");
        }
        if (val == null) {
            val = prefs.get("connectingWithOthers");
        }
        if (val == null) {
            val = prefs.get("connecting_with_others");
        }
        if (val instanceof Boolean b) {
            return b;
        }
        if (val instanceof String s) {
            return !"false".equalsIgnoreCase(s.trim()) && !"off".equalsIgnoreCase(s.trim());
        }
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isProfileUpdatingEnabled(String userIdentifier) {
        if (userIdentifier == null || userIdentifier.isBlank()) {
            return true;
        }
        Map<String, Object> prefs = getNotificationPreferencesForUser(userIdentifier);
        Object val = prefs.get("profile");
        if (val == null) {
            val = prefs.get("updating-your-profile");
        }
        if (val == null) {
            val = prefs.get("updatingYourProfile");
        }
        if (val == null) {
            val = prefs.get("profile_updating");
        }
        if (val == null) {
            val = prefs.get("profile-updates");
        }
        if (val instanceof Boolean b) {
            return b;
        }
        if (val instanceof String s) {
            return !"false".equalsIgnoreCase(s.trim()) && !"off".equalsIgnoreCase(s.trim());
        }
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isPostingAndCommentingEnabled(String userIdentifier) {
        if (userIdentifier == null || userIdentifier.isBlank()) {
            return true;
        }
        Map<String, Object> prefs = getNotificationPreferencesForUser(userIdentifier);
        Object val = prefs.get("posting");
        if (val == null) {
            val = prefs.get("posting-and-commenting");
        }
        if (val == null) {
            val = prefs.get("postingAndCommenting");
        }
        if (val == null) {
            val = prefs.get("posting_and_commenting");
        }
        if (val instanceof Boolean b) {
            return b;
        }
        if (val instanceof String s) {
            return !"false".equalsIgnoreCase(s.trim()) && !"off".equalsIgnoreCase(s.trim());
        }
        return true;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPrivacyPreferencesForUser(String userIdentifier) {
        if (userSettingsRepository == null || userIdentifier == null || userIdentifier.isBlank()) {
            return new LinkedHashMap<>(DEFAULT_PRIVACY_PREFERENCES);
        }
        String key = resolveSettingsKey(userIdentifier);
        return userSettingsRepository.findByUserEmailIgnoreCase(key)
                .map(entity -> mergeWithDefaults(DEFAULT_PRIVACY_PREFERENCES, fromJsonMap(entity.getPrivacyJson())))
                .orElseGet(() -> new LinkedHashMap<>(DEFAULT_PRIVACY_PREFERENCES));
    }

    @Transactional(readOnly = true)
    public boolean isPublicProfileEnabled(String userIdentifier) {
        if (userIdentifier == null || userIdentifier.isBlank()) {
            return true;
        }
        Map<String, Object> prefs = getPrivacyPreferencesForUser(userIdentifier);
        Object val = prefs.get("profileVisibility");
        if (val == null) {
            val = prefs.get("publicProfile");
        }
        if (val == null) {
            val = prefs.get("public-profile");
        }
        if (val == null) {
            val = prefs.get("profile_visibility");
        }
        if (val instanceof Boolean b) {
            return b;
        }
        if (val instanceof String s) {
            return !"false".equalsIgnoreCase(s.trim()) && !"off".equalsIgnoreCase(s.trim());
        }
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isPostRestrictedByAuthorMediaVisibility(PostEntity post) {
        if (post == null || post.isAudienceOverride() || post.getAuthorId() == null || post.getAuthorId().isBlank()) {
            return false;
        }
        Map<String, Object> prefs = getMediaPreferencesForUser(post.getAuthorId());
        return matchesPrivateMediaPreferences(
                prefs,
                post.getMedia(),
                post.getMediaUrl(),
                post.getMediaType(),
                post.getTitle(),
                post.getContent()
        );
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public boolean isPostMapRestrictedByAuthorMediaVisibility(Map<String, Object> postDto, String fallbackAuthorId) {
        if (postDto == null || Boolean.TRUE.equals(postDto.get("audienceOverride"))) {
            return false;
        }
        String authorId = postDto.get("authorId") != null ? String.valueOf(postDto.get("authorId")).trim() : "";
        if (authorId.isEmpty()) {
            authorId = fallbackAuthorId != null ? fallbackAuthorId.trim() : "";
        }
        if (authorId.isEmpty()) {
            return false;
        }
        Map<String, Object> prefs = getMediaPreferencesForUser(authorId);
        Map<String, Object> media = postDto.get("media") instanceof Map<?, ?> m
                ? (Map<String, Object>) m
                : null;
        String mediaUrl = postDto.get("mediaUrl") != null ? String.valueOf(postDto.get("mediaUrl")) : null;
        String mediaType = postDto.get("mediaType") != null ? String.valueOf(postDto.get("mediaType")) : null;
        String title = postDto.get("title") != null ? String.valueOf(postDto.get("title")) : null;
        String content = postDto.get("content") != null ? String.valueOf(postDto.get("content")) : null;
        return matchesPrivateMediaPreferences(prefs, media, mediaUrl, mediaType, title, content);
    }

    private boolean matchesPrivateMediaPreferences(
            Map<String, Object> prefs,
            Map<String, Object> media,
            String mediaUrl,
            String mediaType,
            String title,
            String content
    ) {
        boolean photoPrivate = "private".equalsIgnoreCase(String.valueOf(prefs.get("photoPosts")));
        boolean videoPrivate = "private".equalsIgnoreCase(String.valueOf(prefs.get("videoPosts")));
        boolean urlPrivate = "private".equalsIgnoreCase(String.valueOf(prefs.get("externalUrls")));
        if (!photoPrivate && !videoPrivate && !urlPrivate) {
            return false;
        }

        boolean isVideo = isVideoMedia(media, mediaUrl, mediaType);
        boolean isPhoto = isPhotoMedia(media, mediaUrl, mediaType, isVideo);
        boolean hasUrl = hasExternalUrl(title, content);

        if (photoPrivate && isPhoto) {
            return true;
        }
        if (videoPrivate && isVideo) {
            return true;
        }
        if (urlPrivate && hasUrl) {
            return true;
        }
        return false;
    }

    private boolean isVideoMedia(Map<String, Object> media, String mediaUrl, String mediaType) {
        String resolvedUrl = resolveMediaUrl(media, mediaUrl);
        if (resolvedUrl.isEmpty()) return false;
        String kindLower = resolveMediaKind(media, mediaType);
        String contentTypeLower = (media != null && media.get("contentType") != null)
                ? String.valueOf(media.get("contentType")).trim().toLowerCase(Locale.ROOT)
                : "";
        String fileNameLower = (media != null && media.get("fileName") != null)
                ? String.valueOf(media.get("fileName")).trim().toLowerCase(Locale.ROOT)
                : "";
        String urlLower = resolvedUrl.toLowerCase(Locale.ROOT);
        return kindLower.contains("video")
                || contentTypeLower.startsWith("video/")
                || urlLower.startsWith("data:video/")
                || urlLower.matches(".*\\.(mp4|webm|mov|m4v|avi|mkv|ogg|ogv|3gp|flv|wmv|mpeg|mpg)(\\?.*)?$")
                || fileNameLower.matches(".*\\.(mp4|webm|mov|m4v|avi|mkv|ogg|ogv|3gp|flv|wmv|mpeg|mpg)$");
    }

    private boolean isPhotoMedia(Map<String, Object> media, String mediaUrl, String mediaType, boolean isVideo) {
        String resolvedUrl = resolveMediaUrl(media, mediaUrl);
        if (resolvedUrl.isEmpty() || isVideo) return false;
        String kindLower = resolveMediaKind(media, mediaType);
        String urlLower = resolvedUrl.toLowerCase(Locale.ROOT);
        return kindLower.contains("image")
                || kindLower.contains("photo")
                || urlLower.startsWith("data:image/")
                || urlLower.matches(".*\\.(jpg|jpeg|png|gif|webp|avif|bmp|svg)(\\?.*)?$")
                || (!kindLower.contains("audio") && !kindLower.contains("pdf") && !kindLower.contains("doc"));
    }

    private boolean hasExternalUrl(String title, String content) {
        return (content != null && EXTERNAL_URL_PATTERN.matcher(content).find())
                || (title != null && EXTERNAL_URL_PATTERN.matcher(title).find());
    }

    private String resolveMediaUrl(Map<String, Object> media, String mediaUrl) {
        String resolvedUrl = mediaUrl != null ? mediaUrl.trim() : "";
        if (resolvedUrl.isEmpty() && media != null && media.get("url") != null) {
            resolvedUrl = String.valueOf(media.get("url")).trim();
        }
        return resolvedUrl;
    }

    private String resolveMediaKind(Map<String, Object> media, String mediaType) {
        String resolvedKind = "";
        if (media != null) {
            if (media.get("kind") != null) resolvedKind = String.valueOf(media.get("kind"));
            else if (media.get("type") != null) resolvedKind = String.valueOf(media.get("type"));
            else if (media.get("contentType") != null) resolvedKind = String.valueOf(media.get("contentType"));
        }
        if (resolvedKind.isBlank() && mediaType != null) {
            resolvedKind = mediaType;
        }
        return resolvedKind.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public Map<String, Object> changePassword(PasswordChangeRequest req, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new BadRequestException("User email header required");
        }

        Optional<UserAccountEntity> opt = resolveUserAccount(userEmail);
        if (opt.isEmpty()) {
            throw new UnauthorizedException("User not found");
        }

        UserAccountEntity user = opt.get();
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("INVALID_PASSWORD", "Current password does not match.");
        }

        PasswordPolicy.requireValid(req.getNewPassword(), "newPassword");
        PasswordPolicy.requireConfirmed(req.getNewPassword(), req.getConfirmPassword(), false);

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userAccountRepository.save(user);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Password changed successfully.");
        return res;
    }

    @Transactional
    public Map<String, Object> deleteAccount(String userEmail) {
        resolveUserAccount(userEmail)
                .ifPresent(user -> {
                    Set<String> profileIds = new HashSet<>();
                    if (user.getPublicUserId() != null && !user.getPublicUserId().isBlank()) {
                        profileIds.add(user.getPublicUserId());
                    }

                    Map<Long, ProfileEntity> profilesById = new HashMap<>();
                    profileRepository.findAllByEmail(user.getEmail())
                            .forEach(profile -> profilesById.put(profile.getId(), profile));
                    if (!profileIds.isEmpty()) {
                        profileRepository.findAllByUserId(user.getPublicUserId())
                                .forEach(profile -> profilesById.put(profile.getId(), profile));
                    }
                    for (ProfileEntity profile : profilesById.values()) {
                        if (profile.getUserId() != null && !profile.getUserId().isBlank()) {
                            profileIds.add(profile.getUserId());
                        }
                    }

                    if (userSettingsRepository != null && user.getEmail() != null) {
                        userSettingsRepository.deleteByUserEmailIgnoreCase(user.getEmail());
                    }
                    if (blockedUserRepository != null && user.getEmail() != null) {
                        blockedUserRepository.deleteByBlockerEmailIgnoreCase(user.getEmail());
                    }
                    if (savedPostRepository != null && user.getEmail() != null) {
                        savedPostRepository.deleteByUserEmailIgnoreCase(user.getEmail());
                    }
                    if (hiddenContentRepository != null && user.getEmail() != null) {
                        hiddenContentRepository.deleteByUserEmailIgnoreCase(user.getEmail());
                    }

                    if (!profilesById.isEmpty()) {
                        profileRepository.deleteAll(profilesById.values());
                    }
                    profileIds.forEach(networkProfileRepository::deleteById);
                    userAccountRepository.delete(user);
                });
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Account scheduled for deletion.");
        return res;
    }

    private Map<String, Object> mergeWithDefaults(Map<String, Object> defaults, Map<String, Object> stored) {
        Map<String, Object> merged = new LinkedHashMap<>(defaults);
        if (stored != null) {
            merged.putAll(stored);
        }
        return merged;
    }

    private String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> fromJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String firstNonBlank(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) {
                String str = String.valueOf(val).trim();
                if (!str.isEmpty()) {
                    return str;
                }
            }
        }
        return "";
    }
}
