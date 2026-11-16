package com.truehunt.social.service;

import com.truehunt.social.dto.PasswordChangeRequest;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.NetworkProfileRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import com.truehunt.social.validation.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SettingsService {

    private static final Map<String, Object> DEFAULT_NOTIFICATION_PREFERENCES = Map.of(
            "emailNotifications", true,
            "pushNotifications", true,
            "opportunityAlerts", true,
            "messageAlerts", true
    );

    private final UserAccountRepository userAccountRepository;
    private final ProfileRepository profileRepository;
    private final NetworkProfileRepository networkProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, String> userThemes = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> userNotificationPreferences = new ConcurrentHashMap<>();

    public SettingsService(UserAccountRepository userAccountRepository,
                           ProfileRepository profileRepository,
                           NetworkProfileRepository networkProfileRepository,
                           PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.profileRepository = profileRepository;
        this.networkProfileRepository = networkProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private String resolveSettingsKey(String caller) {
        if (caller == null || caller.isBlank()) {
            return "default";
        }
        return resolveUserAccount(caller)
                .map(UserAccountEntity::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .map(email -> email.trim().toLowerCase(Locale.ROOT))
                .orElseGet(() -> caller.trim().toLowerCase(Locale.ROOT));
    }

    private Map<String, Object> getOrCreatePreferences(String key) {
        return userNotificationPreferences.computeIfAbsent(key, ignored -> new ConcurrentHashMap<>(DEFAULT_NOTIFICATION_PREFERENCES));
    }

    public Map<String, Object> getSettings() {
        return getSettings(null);
    }

    public Map<String, Object> getSettings(String caller) {
        String key = resolveSettingsKey(caller);
        Map<String, Object> settings = new HashMap<>();
        settings.put("theme", userThemes.getOrDefault(key, "dark"));
        settings.put("notifications", new HashMap<>(getOrCreatePreferences(key)));
        settings.put("platformVersion", "2.4.0");
        return settings;
    }

    public Map<String, Object> updateTheme(String theme) {
        return updateTheme(theme, null);
    }

    public Map<String, Object> updateTheme(String theme, String caller) {
        String key = resolveSettingsKey(caller);
        String resolvedTheme = (theme != null && !theme.isBlank())
                ? theme
                : userThemes.getOrDefault(key, "dark");
        userThemes.put(key, resolvedTheme);
        Map<String, Object> res = new HashMap<>();
        res.put("theme", resolvedTheme);
        res.put("message", "Theme updated successfully");
        return res;
    }

    public Map<String, Object> updateNotificationPreferences(Map<String, Object> prefs) {
        return updateNotificationPreferences(prefs, null);
    }

    public Map<String, Object> updateNotificationPreferences(Map<String, Object> prefs, String caller) {
        String key = resolveSettingsKey(caller);
        Map<String, Object> userPrefs = getOrCreatePreferences(key);
        if (prefs != null) {
            userPrefs.putAll(prefs);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("notifications", new HashMap<>(userPrefs));
        res.put("message", "Preferences saved successfully");
        return res;
    }

    private Optional<UserAccountEntity> resolveUserAccount(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String trimmed = identifier.trim();
        return userAccountRepository.findByEmail(trimmed.toLowerCase(Locale.ROOT))
                .or(() -> userAccountRepository.findByPublicUserId(trimmed));
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
}
