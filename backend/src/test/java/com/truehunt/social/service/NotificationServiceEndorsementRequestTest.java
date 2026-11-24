package com.truehunt.social.service;

import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class NotificationServiceEndorsementRequestTest {

    private NotificationRepository notificationRepository;
    private ConnectionService connectionService;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        connectionService = mock(ConnectionService.class);
        notificationService = new NotificationService(
                notificationRepository,
                mock(UserAccountRepository.class),
                mock(SettingsService.class),
                connectionService
        );

        when(connectionService.resolveAllUserIdentifiers("owner@example.com"))
                .thenReturn(Set.of("owner@example.com", "owner-public-id"));
        when(connectionService.getConnectedProfiles("owner@example.com"))
                .thenReturn(List.of(Map.of(
                        "userId", "connection-public-id",
                        "name", "Connected Person"
                )));
        when(notificationRepository.save(any(NotificationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void sendsEndorsementRequestOnlyToSelectedConnectionsWithSenderIdentity() {
        List<Map<String, Object>> sent = notificationService.sendEndorsementRequests(
                "owner@example.com",
                "owner-public-id",
                List.of("connection-public-id")
        );

        assertEquals(1, sent.size());
        assertEquals("endorsement_request", sent.get(0).get("type"));
        assertEquals("owner-public-id", sent.get(0).get("senderProfileId"));
        assertEquals("connection-public-id", sent.get(0).get("recipientId"));
        verify(notificationRepository).save(any(NotificationEntity.class));
    }

    @Test
    void rejectsRecipientsWhoAreNotConnections() {
        assertThrows(
                BadRequestException.class,
                () -> notificationService.sendEndorsementRequests(
                        "owner@example.com",
                        "owner-public-id",
                        List.of("not-a-connection")
                )
        );

        verify(notificationRepository, never()).save(any(NotificationEntity.class));
    }

    @Test
    void preventsAnotherRequestUntilTwentyFourHoursHavePassed() {
        NotificationEntity recentRequest = new NotificationEntity(
                "connection-public-id",
                "Endorsement request",
                "A connection has asked you to endorse their profile.",
                "endorsement_request"
        );
        recentRequest.setSenderProfileId("owner-public-id");
        recentRequest.setCreatedAt(LocalDateTime.now().minusHours(23));
        when(notificationRepository.findAll()).thenReturn(List.of(recentRequest));

        assertThrows(
                BadRequestException.class,
                () -> notificationService.sendEndorsementRequests(
                        "owner@example.com",
                        "owner-public-id",
                        List.of("connection-public-id")
                )
        );
    }

    @Test
    void exposesRemainingCooldownForPreviouslyRequestedConnection() {
        NotificationEntity recentRequest = new NotificationEntity(
                "connection-public-id",
                "Endorsement request",
                "A connection has asked you to endorse their profile.",
                "endorsement_request"
        );
        recentRequest.setSenderProfileId("owner-public-id");
        recentRequest.setCreatedAt(LocalDateTime.now().minusHours(23));
        when(notificationRepository.findAll()).thenReturn(List.of(recentRequest));

        List<Map<String, Object>> cooldowns = notificationService.getEndorsementRequestCooldowns(
                "owner@example.com",
                "owner-public-id"
        );

        assertEquals(1, cooldowns.size());
        assertEquals("connection-public-id", cooldowns.get(0).get("recipientProfileId"));
        assertEquals(true, ((Number) cooldowns.get(0).get("remainingSeconds")).longValue() > 0);
    }
}
