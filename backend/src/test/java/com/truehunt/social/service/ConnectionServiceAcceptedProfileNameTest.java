package com.truehunt.social.service;

import com.truehunt.social.dto.ChatMessageDto;
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

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectionServiceAcceptedProfileNameTest {

    @Test
    void sendingAConnectionRequestStoresItsNote() {
        NetworkProfileRepository networkProfiles = mock(NetworkProfileRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        ProfileRepository profiles = mock(ProfileRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserProfileRelationshipRepository relationships = mock(UserProfileRelationshipRepository.class);

        UserAccountEntity requester = account(1L, "requester@example.com", "requester-id", "Ravi Kumar");
        when(accounts.findByEmail("requester@example.com")).thenReturn(Optional.of(requester));
        when(accounts.findByPublicUserId("requester-id")).thenReturn(Optional.of(requester));
        when(relationships.findAll()).thenReturn(List.of());

        ConnectionService service = new ConnectionService(
                networkProfiles,
                notifications,
                profiles,
                accounts,
                relationships,
                mock(MessageService.class),
                null,
                null);

        service.sendConnectionRequest("target-id", " Would love to connect. ", "requester@example.com");

        ArgumentCaptor<UserProfileRelationshipEntity> relationshipCaptor =
                ArgumentCaptor.forClass(UserProfileRelationshipEntity.class);
        verify(relationships, times(2)).save(relationshipCaptor.capture());
        assertEquals(
                List.of("Would love to connect.", "Would love to connect."),
                relationshipCaptor.getAllValues().stream()
                        .map(UserProfileRelationshipEntity::getConnectionNote)
                        .toList()
        );
    }

    @Test
    void acceptingRequestResolvesCorrectProfilesForBothPeople() {
        NetworkProfileRepository networkProfiles = mock(NetworkProfileRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        ProfileRepository profiles = mock(ProfileRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserProfileRelationshipRepository relationships = mock(UserProfileRelationshipRepository.class);

        UserAccountEntity requester = account(1L, "requester@example.com", "requester-id", "Ravi Kumar");
        UserAccountEntity acceptor = account(2L, "acceptor@example.com", "acceptor-id", "Asha Rao");
        when(accounts.findByEmail("requester@example.com")).thenReturn(Optional.of(requester));
        when(accounts.findByEmail("acceptor@example.com")).thenReturn(Optional.of(acceptor));
        when(accounts.findByPublicUserId("requester-id")).thenReturn(Optional.of(requester));
        when(accounts.findByPublicUserId("acceptor-id")).thenReturn(Optional.of(acceptor));

        ProfileEntity requesterProfile = profile("requester-id", "requester@example.com", "Ravi Kumar");
        ProfileEntity acceptorProfile = profile("acceptor-id", "acceptor@example.com", "TrueHunt Member");
        when(profiles.findByUserId("requester-id")).thenReturn(Optional.of(requesterProfile));
        when(profiles.findByUserId("acceptor-id")).thenReturn(Optional.of(acceptorProfile));
        when(profiles.findByEmail("requester@example.com")).thenReturn(Optional.of(requesterProfile));
        when(profiles.findByEmail("acceptor@example.com")).thenReturn(Optional.of(acceptorProfile));

        UserProfileRelationshipEntity relationship = new UserProfileRelationshipEntity();
        relationship.setOwnerUserId("1");
        relationship.setTargetProfileId("2");
        relationship.setSenderUserId("requester-id");
        relationship.setStatus("PENDING");
        relationship.setConnectionNote("Hi");
        when(relationships.findAll()).thenReturn(List.of(relationship));
        MessageService messages = mock(MessageService.class);

        ConnectionService service = new ConnectionService(
                networkProfiles,
                notifications,
                profiles,
                accounts,
                relationships,
                messages,
                null,
                null);

        service.acceptConnection("requester-id", "acceptor@example.com");

        assertEquals("requester-id", relationship.getOwnerUserId());
        assertEquals("acceptor@example.com", relationship.getTargetProfileId());
        ArgumentCaptor<ChatMessageDto> messageCaptor = ArgumentCaptor.forClass(ChatMessageDto.class);
        ArgumentCaptor<Principal> principalCaptor = ArgumentCaptor.forClass(Principal.class);
        verify(messages).processAndSendMessage(messageCaptor.capture(), principalCaptor.capture());
        assertEquals("Hi", messageCaptor.getValue().getContent());
        assertEquals("acceptor-id", messageCaptor.getValue().getReceiverId());
        assertEquals("requester-id", principalCaptor.getValue().getName());
        ArgumentCaptor<NotificationEntity> notificationCaptor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notifications, times(2)).save(notificationCaptor.capture());
        assertEquals("Connection Request Accepted", notificationCaptor.getAllValues().get(0).getTitle());
        assertEquals("requester-id", notificationCaptor.getAllValues().get(0).getRecipientId());
        assertEquals(
                "Asha Rao accepted your connection request on TrueHunt.",
                notificationCaptor.getAllValues().get(0).getMessage());
        assertConnection(service.getConnectedProfiles("requester@example.com"), "acceptor-id", "Asha Rao");
        assertConnection(service.getConnectedProfiles("acceptor@example.com"), "requester-id", "Ravi Kumar");
    }

    @Test
    void rejectingRequestNotifiesTheRequester() {
        NetworkProfileRepository networkProfiles = mock(NetworkProfileRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        ProfileRepository profiles = mock(ProfileRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserProfileRelationshipRepository relationships = mock(UserProfileRelationshipRepository.class);

        UserAccountEntity requester = account(1L, "requester@example.com", "requester-id", "Ravi Kumar");
        UserAccountEntity rejector = account(2L, "rejector@example.com", "rejector-id", "Asha Rao");
        when(accounts.findByEmail("requester@example.com")).thenReturn(Optional.of(requester));
        when(accounts.findByEmail("rejector@example.com")).thenReturn(Optional.of(rejector));
        when(accounts.findByPublicUserId("requester-id")).thenReturn(Optional.of(requester));
        when(profiles.findByUserId("requester-id"))
                .thenReturn(Optional.of(profile("requester-id", "requester@example.com", "Ravi Kumar")));
        when(profiles.findByUserId("rejector-id"))
                .thenReturn(Optional.of(profile("rejector-id", "rejector@example.com", "Asha Rao")));
        when(relationships.findAll()).thenReturn(List.of());

        ConnectionService service = new ConnectionService(
                networkProfiles,
                notifications,
                profiles,
                accounts,
                relationships,
                mock(MessageService.class),
                null,
                null);

        service.rejectConnection("requester-id", "rejector@example.com");

        ArgumentCaptor<NotificationEntity> notificationCaptor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notifications).save(notificationCaptor.capture());
        NotificationEntity notification = notificationCaptor.getValue();
        assertEquals("requester-id", notification.getRecipientId());
        assertEquals("Connection Request Rejected", notification.getTitle());
        assertEquals("Asha Rao rejected your connection request on TrueHunt.", notification.getMessage());
        assertEquals("connection_rejected", notification.getType());
    }

    @Test
    void incomingInvitationUsesRegisteredAccountDetailsWhenProfileIsGeneric() {
        NetworkProfileRepository networkProfiles = mock(NetworkProfileRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        ProfileRepository profiles = mock(ProfileRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserProfileRelationshipRepository relationships = mock(UserProfileRelationshipRepository.class);

        UserAccountEntity recipient = account(2L, "recipient@example.com", "recipient-id", "Recipient");
        UserAccountEntity sender = account(1L, "sender@example.com", "sender-id", "Sender Name");
        sender.setSubCraft("Film Director");
        sender.setHeadquarters("Toronto");
        when(accounts.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipient));
        when(accounts.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));

        ProfileEntity senderProfile = profile("sender-id", "sender@example.com", "Profile");
        when(profiles.findByUserId("sender-id")).thenReturn(Optional.of(senderProfile));
        when(profiles.findByEmail("sender@example.com")).thenReturn(Optional.of(senderProfile));

        UserProfileRelationshipEntity invitation = new UserProfileRelationshipEntity();
        invitation.setOwnerUserId("recipient-id");
        invitation.setTargetProfileId("sender-id");
        invitation.setSenderUserId("sender-id");
        invitation.setStatus("PENDING");
        invitation.setConnectionNote("Would love to connect.");
        when(relationships.findAll()).thenReturn(List.of(invitation));

        ConnectionService service = new ConnectionService(
                networkProfiles,
                notifications,
                profiles,
                accounts,
                relationships,
                mock(MessageService.class),
                null,
                null);

        List<Map<String, Object>> invitations = service.getInvitations("recipient@example.com");

        assertEquals(1, invitations.size());
        assertEquals("Sender Name", invitations.get(0).get("name"));
        assertEquals("Film Director", invitations.get(0).get("role"));
        assertEquals("Toronto", invitations.get(0).get("location"));
        assertEquals("Would love to connect.", invitations.get(0).get("message"));
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

    private static void assertConnection(
            List<Map<String, Object>> profiles,
            String expectedId,
            String expectedName) {
        assertEquals(1, profiles.size());
        assertEquals(expectedId, profiles.get(0).get("id"));
        assertEquals(expectedName, profiles.get(0).get("name"));
    }
}
