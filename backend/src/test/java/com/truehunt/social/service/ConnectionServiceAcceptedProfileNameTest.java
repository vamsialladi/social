package com.truehunt.social.service;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConnectionServiceAcceptedProfileNameTest {

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
        when(relationships.findAll()).thenReturn(List.of(relationship));

        ConnectionService service = new ConnectionService(
                networkProfiles,
                notifications,
                profiles,
                accounts,
                relationships,
                null,
                null);

        service.acceptConnection("requester-id", "acceptor@example.com");

        assertEquals("requester-id", relationship.getOwnerUserId());
        assertEquals("acceptor@example.com", relationship.getTargetProfileId());
        assertConnection(service.getConnectedProfiles("requester@example.com"), "acceptor-id", "Asha Rao");
        assertConnection(service.getConnectedProfiles("acceptor@example.com"), "requester-id", "Ravi Kumar");
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
