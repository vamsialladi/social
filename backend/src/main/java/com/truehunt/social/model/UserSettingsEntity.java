package com.truehunt.social.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings")
public class UserSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", unique = true, nullable = false, length = 255)
    private String userEmail;

    @Column(nullable = false, length = 32)
    private String theme = "dark";

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notificationsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String privacyJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String restrictedJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String mediaJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String engagementJson;

    @Column(nullable = false)
    private boolean twoFactorEnabled = false;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public UserSettingsEntity() {}

    public UserSettingsEntity(String userEmail) {
        this.userEmail = userEmail;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getNotificationsJson() { return notificationsJson; }
    public void setNotificationsJson(String notificationsJson) { this.notificationsJson = notificationsJson; }

    public String getPrivacyJson() { return privacyJson; }
    public void setPrivacyJson(String privacyJson) { this.privacyJson = privacyJson; }

    public String getRestrictedJson() { return restrictedJson; }
    public void setRestrictedJson(String restrictedJson) { this.restrictedJson = restrictedJson; }

    public String getMediaJson() { return mediaJson; }
    public void setMediaJson(String mediaJson) { this.mediaJson = mediaJson; }

    public String getEngagementJson() { return engagementJson; }
    public void setEngagementJson(String engagementJson) { this.engagementJson = engagementJson; }

    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
