package com.truehunt.social.service;

import com.truehunt.social.model.BirthdayNotificationDispatchEntity;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.repository.BirthdayNotificationDispatchRepository;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.ProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BirthdayNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BirthdayNotificationService.class);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH);

    private final ProfileRepository profileRepository;
    private final ConnectionService connectionService;
    private final BirthdayNotificationDispatchRepository dispatchRepository;
    private final NotificationRepository notificationRepository;
    private final SettingsService settingsService;
    private final String timeZone;
    private final LocalTime notificationTime;

    public BirthdayNotificationService(
            ProfileRepository profileRepository,
            ConnectionService connectionService,
            BirthdayNotificationDispatchRepository dispatchRepository,
            NotificationRepository notificationRepository,
            SettingsService settingsService,
            @Value("${app.birthday.notification-time-zone:Asia/Kolkata}") String timeZone,
            @Value("${app.birthday.notification-time:09:00}") String notificationTime
    ) {
        this.profileRepository = profileRepository;
        this.connectionService = connectionService;
        this.dispatchRepository = dispatchRepository;
        this.notificationRepository = notificationRepository;
        this.settingsService = settingsService;
        this.timeZone = timeZone;
        this.notificationTime = LocalTime.parse(notificationTime);
    }

    @Scheduled(
            cron = "${app.birthday.notification-cron:0 * * * * *}",
            zone = "${app.birthday.notification-time-zone:Asia/Kolkata}"
    )
    @Transactional
    public void sendScheduledBirthdayNotifications() {
        sendBirthdayNotificationsAt(LocalDateTime.now(ZoneId.of(timeZone)));
    }

    @Transactional
    void sendBirthdayNotificationsAt(LocalDateTime now) {
        if (now.toLocalTime().isBefore(notificationTime)) {
            return;
        }

        LocalDate today = now.toLocalDate();
        for (ProfileEntity profile : profileRepository.findAll()) {
            LocalDate dateOfBirth = parseDateOfBirth(profile);
            if (dateOfBirth == null || profile.getUserId() == null || profile.getUserId().isBlank()) {
                continue;
            }

            for (int year = today.getYear(); year <= today.getYear() + 1; year++) {
                LocalDate birthday = observedBirthday(dateOfBirth, year);
                if (birthday.minusDays(1).equals(today)) {
                    sendBirthdayNotifications(profile, birthday, today, true);
                } else if (birthday.equals(today)) {
                    sendBirthdayNotifications(profile, birthday, today, false);
                }
            }
        }
    }

    private LocalDate parseDateOfBirth(ProfileEntity profile) {
        String value = profile.getDateOfBirth();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            LOGGER.warn("Skipping birthday notifications for profile {} with invalid date of birth", profile.getUserId());
            return null;
        }
    }

    private LocalDate observedBirthday(LocalDate dateOfBirth, int year) {
        if (dateOfBirth.getMonthValue() == 2
                && dateOfBirth.getDayOfMonth() == 29
                && !Year.isLeap(year)) {
            return LocalDate.of(year, 2, 28);
        }
        return LocalDate.of(year, dateOfBirth.getMonth(), dateOfBirth.getDayOfMonth());
    }

    private void sendBirthdayNotifications(
            ProfileEntity birthdayProfile,
            LocalDate birthday,
            LocalDate notificationDate,
            boolean dayBefore
    ) {
        String birthdayUserId = birthdayProfile.getUserId().trim();
        Map<String, String> recipients = new LinkedHashMap<>();
        recipients.put(birthdayUserId, birthdayProfile.getName());

        if (settingsService.isPublicProfileEnabled(birthdayUserId)) {
            String connectionLookupId = birthdayProfile.getEmail() == null || birthdayProfile.getEmail().isBlank()
                    ? birthdayUserId
                    : birthdayProfile.getEmail();
            for (Map<String, Object> connection : connectionService.getConnectedProfiles(connectionLookupId)) {
                Object connectionId = connection.get("id") != null
                        ? connection.get("id")
                        : connection.get("profileId");
                if (connectionId == null || connectionId.toString().isBlank()) {
                    continue;
                }
                String recipientId = connectionId.toString().trim();
                if (!recipientId.equalsIgnoreCase(birthdayUserId)) {
                    Object connectionName = connection.get("name");
                    recipients.putIfAbsent(
                            recipientId,
                            connectionName == null ? "Your connection" : connectionName.toString()
                    );
                }
            }
        }

        String scheduledDate = birthday.format(DATE_FORMAT);
        for (Map.Entry<String, String> recipient : recipients.entrySet()) {
            String recipientId = recipient.getKey();
            if (dispatchRepository.existsByBirthdayOwnerUserIdAndRecipientIdAndNotificationDate(
                    birthdayUserId,
                    recipientId,
                    notificationDate
            )) {
                continue;
            }

            boolean isBirthdayUser = recipientId.equalsIgnoreCase(birthdayUserId);
            String title = notificationTitle(isBirthdayUser, dayBefore);
            String message = notificationMessage(
                    birthdayProfile.getName(),
                    scheduledDate,
                    isBirthdayUser,
                    dayBefore
            );
            String type = isBirthdayUser ? "birthday_reminder" : "connection_birthday";

            dispatchRepository.save(new BirthdayNotificationDispatchEntity(
                    birthdayUserId,
                    recipientId,
                    notificationDate
            ));
            notificationRepository.save(new NotificationEntity(
                    recipientId,
                    title,
                    message,
                    type,
                    birthdayUserId
            ));
        }
    }

    private String notificationTitle(boolean isBirthdayUser, boolean dayBefore) {
        if (isBirthdayUser) {
            return dayBefore ? "Your birthday is tomorrow" : "Happy birthday!";
        }
        return dayBefore ? "Birthday tomorrow" : "Birthday today";
    }

    private String notificationMessage(
            String birthdayName,
            String scheduledDate,
            boolean isBirthdayUser,
            boolean dayBefore
    ) {
        if (isBirthdayUser) {
            return dayBefore
                    ? "Your birthday is tomorrow, " + scheduledDate + "."
                    : "Happy birthday! Today is your birthday.";
        }
        String name = birthdayName == null || birthdayName.isBlank() ? "Your connection" : birthdayName;
        return dayBefore
                ? "Tomorrow, " + scheduledDate + ", is " + name + "'s birthday. Send them your wishes!"
                : "Today is " + name + "'s birthday. Wish them a happy birthday!";
    }
}
