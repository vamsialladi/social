package com.truehunt.social.service;

import com.truehunt.social.model.BirthdayNotificationDispatchEntity;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.repository.BirthdayNotificationDispatchRepository;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BirthdayNotificationServiceTest {

    @Test
    void sendsDayBeforeBirthdayNotificationsToUserAndAcceptedConnections() {
        TestContext context = new TestContext();
        LocalDate today = LocalDate.of(2026, 11, 27);
        ProfileEntity birthdayUser = profile("birthday-user", "Anu", "1990-11-28");
        ProfileEntity connection = profile("connection-user", "Sam", null);

        when(context.profiles.findAll()).thenReturn(List.of(birthdayUser, connection));
        when(context.connectionService.getConnectedProfiles("birthday-user@example.com"))
                .thenReturn(List.of(Map.of("id", "connection-user", "name", "Sam")));
        when(context.settings.isPublicProfileEnabled("birthday-user")).thenReturn(true);

        context.service.sendBirthdayNotificationsAt(today.atTime(9, 0));

        ArgumentCaptor<NotificationEntity> notifications = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(context.notificationRepository, org.mockito.Mockito.times(2)).save(notifications.capture());
        assertEquals(
                List.of("birthday-user", "connection-user"),
                notifications.getAllValues().stream().map(NotificationEntity::getRecipientId).toList()
        );
        assertTrue(notifications.getAllValues().get(0).getMessage().contains("Your birthday is tomorrow"));
        assertTrue(notifications.getAllValues().get(1).getMessage().contains("November 28"));
        assertEquals("connection_birthday", notifications.getAllValues().get(1).getType());
        verify(context.dispatchRepository, org.mockito.Mockito.times(2))
                .save(any(BirthdayNotificationDispatchEntity.class));
    }

    @Test
    void sendsBirthdayDayNotificationAndDoesNotSendAgainForTheSameRecipientAndDate() {
        TestContext context = new TestContext();
        LocalDate today = LocalDate.of(2026, 11, 28);
        ProfileEntity birthdayUser = profile("birthday-user", "Anu", "1990-11-28");
        when(context.profiles.findAll()).thenReturn(List.of(birthdayUser));
        when(context.dispatchRepository.existsByBirthdayOwnerUserIdAndRecipientIdAndNotificationDate(
                "birthday-user",
                "birthday-user",
                today
        )).thenReturn(false, true);

        context.service.sendBirthdayNotificationsAt(today.atTime(9, 0));
        context.service.sendBirthdayNotificationsAt(today.atTime(9, 1));

        ArgumentCaptor<NotificationEntity> notification = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(context.notificationRepository).save(notification.capture());
        assertEquals("Happy birthday!", notification.getValue().getTitle());
        assertEquals("birthday_reminder", notification.getValue().getType());
        assertEquals("birthday-user", notification.getValue().getRecipientId());
    }

    @Test
    void suppressesConnectionNotificationsWhenBirthdayUserProfileIsPrivate() {
        TestContext context = new TestContext();
        LocalDate today = LocalDate.of(2026, 11, 27);
        ProfileEntity birthdayUser = profile("birthday-user", "Anu", "1990-11-28");
        when(context.profiles.findAll()).thenReturn(List.of(birthdayUser));
        when(context.connectionService.getConnectedProfiles("birthday-user@example.com"))
                .thenReturn(List.of(Map.of("id", "connection-user", "name", "Sam")));
        when(context.settings.isPublicProfileEnabled("birthday-user")).thenReturn(false);

        context.service.sendBirthdayNotificationsAt(today.atTime(9, 0));

        ArgumentCaptor<NotificationEntity> notification = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(context.notificationRepository).save(notification.capture());
        assertEquals("birthday-user", notification.getValue().getRecipientId());
    }

    @Test
    void treatsFebruaryTwentyNinthBirthdayAsFebruaryTwentyEighthInNonLeapYears() {
        TestContext context = new TestContext();
        LocalDate dayBefore = LocalDate.of(2027, 2, 27);
        ProfileEntity birthdayUser = profile("birthday-user", "Anu", "2000-02-29");
        when(context.profiles.findAll()).thenReturn(List.of(birthdayUser));

        context.service.sendBirthdayNotificationsAt(dayBefore.atTime(9, 0));

        ArgumentCaptor<NotificationEntity> notification = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(context.notificationRepository).save(notification.capture());
        assertTrue(notification.getValue().getMessage().contains("February 28"));
    }

    @Test
    void sendsBirthdayNotificationsAgainTheFollowingYear() {
        TestContext context = new TestContext();
        ProfileEntity birthdayUser = profile("birthday-user", "Anu", "1990-11-28");
        when(context.profiles.findAll()).thenReturn(List.of(birthdayUser));

        context.service.sendBirthdayNotificationsAt(LocalDateTime.of(2026, 11, 27, 9, 0));
        context.service.sendBirthdayNotificationsAt(LocalDateTime.of(2027, 11, 27, 9, 0));

        ArgumentCaptor<BirthdayNotificationDispatchEntity> dispatches =
                ArgumentCaptor.forClass(BirthdayNotificationDispatchEntity.class);
        verify(context.dispatchRepository, org.mockito.Mockito.times(2)).save(dispatches.capture());
        assertEquals(
                List.of(LocalDate.of(2026, 11, 27), LocalDate.of(2027, 11, 27)),
                dispatches.getAllValues().stream()
                        .map(BirthdayNotificationDispatchEntity::getNotificationDate)
                        .toList()
        );
        verify(context.notificationRepository, org.mockito.Mockito.times(2))
                .save(any(NotificationEntity.class));
    }

    @Test
    void waitsUntilConfiguredLocalNotificationTime() {
        TestContext context = new TestContext();

        context.service.sendBirthdayNotificationsAt(LocalDateTime.of(2026, 11, 27, 8, 59));

        verifyNoInteractions(context.profiles, context.connectionService, context.dispatchRepository,
                context.notificationRepository);
    }

    private ProfileEntity profile(String userId, String name, String dateOfBirth) {
        ProfileEntity profile = new ProfileEntity();
        profile.setUserId(userId);
        profile.setEmail(userId + "@example.com");
        profile.setName(name);
        profile.setDateOfBirth(dateOfBirth);
        return profile;
    }

    private static class TestContext {
        private final ProfileRepository profiles = mock(ProfileRepository.class);
        private final ConnectionService connectionService = mock(ConnectionService.class);
        private final BirthdayNotificationDispatchRepository dispatchRepository =
                mock(BirthdayNotificationDispatchRepository.class);
        private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
        private final SettingsService settings = mock(SettingsService.class);
        private final BirthdayNotificationService service = new BirthdayNotificationService(
                profiles,
                connectionService,
                dispatchRepository,
                notificationRepository,
                settings,
                "Asia/Kolkata",
                "09:00"
        );
    }
}
