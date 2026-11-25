package com.truehunt.social.service;

import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.model.UserProfileRelationshipEntity;
import com.truehunt.social.repository.NetworkProfileRepository;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import com.truehunt.social.repository.UserProfileRelationshipRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectionRequestCancellationTest {

    @Test
    void withdrawingOutgoingRequestClearsRelationshipAndItsNotifications() {
        NetworkProfileRepository networkProfiles = mock(NetworkProfileRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        ProfileRepository profiles = mock(ProfileRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserProfileRelationshipRepository relationships = mock(UserProfileRelationshipRepository.class);
        UserAccountEntity requester = account(1L, "requester@example.com", "requester-id", "Ravi Kumar");
        UserAccountEntity recipient = account(2L, "recipient@example.com", "recipient-id", "Asha Rao");
        when(accounts.findByEmail("requester@example.com")).thenReturn(Optional.of(requester));
        when(accounts.findByPublicUserId("requester-id")).thenReturn(Optional.of(requester));
        when(accounts.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipient));
        when(accounts.findByEmail("recipient-id")).thenReturn(Optional.empty());
        when(accounts.findByPublicUserId("recipient-id")).thenReturn(Optional.of(recipient));
        when(profiles.findByUserId("requester-id"))
                .thenReturn(Optional.of(profile("requester-id", "requester@example.com", "Ravi Kumar")));
        when(profiles.findByUserId("recipient-id"))
                .thenReturn(Optional.of(profile("recipient-id", "recipient@example.com", "Asha Rao")));

        UserProfileRelationshipEntity requesterRelationship = relationship(
                "requester-id", "recipient-id", "requester-id");
        UserProfileRelationshipEntity recipientRelationship = relationship(
                "recipient-id", "requester-id", "requester-id");
        when(relationships.findAll()).thenReturn(List.of(requesterRelationship, recipientRelationship));

        NotificationEntity recipientNotification = new NotificationEntity(
                "recipient-id",
                "New Connection Request",
                "Ravi Kumar sent you an invitation to connect on TrueHunt.",
                "connection_request");
        NotificationEntity requesterNotification = new NotificationEntity(
                "requester-id",
                "Invitation Sent",
                "You sent a connection invitation to Asha Rao.",
                "connection_request");
        when(notifications.findAllByRecipientIdInAndTypeOrderByCreatedAtDesc(
                anyList(), eq("connection_request")))
                .thenReturn(List.of(recipientNotification, requesterNotification));

        ConnectionService service = service(networkProfiles, notifications, profiles, accounts, relationships);

        Map<String, Object> response = service.cancelConnectionRequest("recipient-id", "requester@example.com");

        assertEquals("Connection request withdrawn", response.get("message"));
        assertNull(requesterRelationship.getStatus());
        assertNull(recipientRelationship.getStatus());
        assertEquals(0, service.getInvitations("recipient@example.com").size());
        verify(notifications).delete(recipientNotification);
        verify(notifications).delete(requesterNotification);
    }

    @Test
    void incomingRequestCannotBeWithdrawnByItsRecipient() {
        NetworkProfileRepository networkProfiles = mock(NetworkProfileRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        ProfileRepository profiles = mock(ProfileRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserProfileRelationshipRepository relationships = mock(UserProfileRelationshipRepository.class);
        UserAccountEntity recipient = account(2L, "recipient@example.com", "recipient-id", "Asha Rao");
        UserAccountEntity requester = account(1L, "requester@example.com", "requester-id", "Ravi Kumar");
        when(accounts.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipient));
        when(accounts.findByPublicUserId("recipient-id")).thenReturn(Optional.of(recipient));
        when(accounts.findByEmail("requester-id")).thenReturn(Optional.empty());
        when(accounts.findByPublicUserId("requester-id")).thenReturn(Optional.of(requester));

        UserProfileRelationshipEntity incomingRequest = relationship(
                "recipient-id", "requester-id", "requester-id");
        when(relationships.findAll()).thenReturn(List.of(incomingRequest));
        ConnectionService service = service(networkProfiles, notifications, profiles, accounts, relationships);

        assertThrows(
                BadRequestException.class,
                () -> service.cancelConnectionRequest("requester-id", "recipient@example.com"));

        verify(relationships, never()).save(incomingRequest);
        verify(notifications, never()).delete(org.mockito.ArgumentMatchers.any(NotificationEntity.class));
    }

    private static ConnectionService service(
            NetworkProfileRepository networkProfiles,
            NotificationRepository notifications,
            ProfileRepository profiles,
            UserAccountRepository accounts,
            UserProfileRelationshipRepository relationships) {
        return new ConnectionService(
                networkProfiles,
                notifications,
                profiles,
                accounts,
                relationships,
                mock(MessageService.class),
                null,
                null);
    }

    private static UserAccountEntity account(Long id, String email, String publicUserId, String name) {
        UserAccountEntity account = new UserAccountEntity();
        account.setId(id);
        account.setEmail(email);
        account.setPublicUserId(publicUserId);
        account.setName(name);
        return account;
    }

    private static ProfileEntity profile(String userId, String email, String name) {
        ProfileEntity profile = new ProfileEntity();
        profile.setUserId(userId);
        profile.setEmail(email);
        profile.setName(name);
        return profile;
    }

    private static UserProfileRelationshipEntity relationship(String ownerId, String targetId, String senderId) {
        UserProfileRelationshipEntity relationship = new UserProfileRelationshipEntity();
        relationship.setOwnerUserId(ownerId);
        relationship.setTargetProfileId(targetId);
        relationship.setSenderUserId(senderId);
        relationship.setStatus("PENDING");
        return relationship;
    }
}
