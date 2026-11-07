package com.truehunt.social.service;

import com.truehunt.social.dto.PasswordChangeRequest;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.NetworkProfileRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class SettingsService {

    private final UserAccountRepository userAccountRepository;
    private final ProfileRepository profileRepository;
    private final NetworkProfileRepository networkProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private String currentTheme = "dark";
    private final Map<String, Object> notificationPreferences = new HashMap<>(Map.of(
            "emailNotifications", true,
            "pushNotifications", true,
            "opportunityAlerts", true,
            "messageAlerts", true
    ));

    public SettingsService(UserAccountRepository userAccountRepository,
                           ProfileRepository profileRepository,
                           NetworkProfileRepository networkProfileRepository,
                           PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.profileRepository = profileRepository;
        this.networkProfileRepository = networkProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("theme", currentTheme);
        settings.put("notifications", notificationPreferences);
        settings.put("platformVersion", "2.4.0");
        return settings;
    }

    public Map<String, Object> updateTheme(String theme) {
        if (theme != null && !theme.isBlank()) {
            this.currentTheme = theme;
        }
        Map<String, Object> res = new HashMap<>();
        res.put("theme", currentTheme);
        res.put("message", "Theme updated successfully");
        return res;
    }

    public Map<String, Object> updateNotificationPreferences(Map<String, Object> prefs) {
        if (prefs != null) {
            this.notificationPreferences.putAll(prefs);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("notifications", notificationPreferences);
        res.put("message", "Preferences saved successfully");
        return res;
    }

    @Transactional
    public Map<String, Object> changePassword(PasswordChangeRequest req, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new BadRequestException("User email header required");
        }

        Optional<UserAccountEntity> opt = userAccountRepository.findByEmail(userEmail.trim().toLowerCase(Locale.ROOT));
        if (opt.isEmpty()) {
            throw new UnauthorizedException("User not found");
        }

        UserAccountEntity user = opt.get();
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("INVALID_PASSWORD", "Current password does not match.");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userAccountRepository.save(user);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Password changed successfully.");
        return res;
    }

    @Transactional
    public Map<String, Object> deleteAccount(String userEmail) {
        if (userEmail != null && !userEmail.isBlank()) {
            userAccountRepository.findByEmail(userEmail.trim().toLowerCase(Locale.ROOT))
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
        }
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Account scheduled for deletion.");
        return res;
    }
}
