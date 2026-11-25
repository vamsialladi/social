package com.truehunt.social.service;

import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.model.UserProfileRelationshipEntity;
import com.truehunt.social.repository.NetworkProfileRepository;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import com.truehunt.social.repository.UserProfileRelationshipRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MutualConnectionsCountTest {

    @Test
    void countsSharedConnectionsOnceEvenWhenRelationshipExistsInBothDirections() {
        NetworkProfileRepository networkProfiles = mock(NetworkProfileRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        ProfileRepository profiles = mock(ProfileRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserProfileRelationshipRepository relationships = mock(UserProfileRelationshipRepository.class);

        UserAccountEntity viewer = account(1L, "viewer@example.com", "viewer-id");
        UserAccountEntity target = account(2L, "target@example.com", "target-id");
        when(accounts.findByEmail("viewer@example.com")).thenReturn(Optional.of(viewer));
        when(accounts.findByPublicUserId("viewer-id")).thenReturn(Optional.of(viewer));
        when(accounts.findByEmail("target-id")).thenReturn(Optional.empty());
        when(accounts.findByPublicUserId("target-id")).thenReturn(Optional.of(target));

        when(relationships.findAll()).thenReturn(List.of(
                relationship("viewer-id", "shared-id"),
                relationship("shared-id", "viewer-id"),
                relationship("target-id", "shared-id"),
                following("follower-id", "target-id")));

        ConnectionService service = new ConnectionService(
                networkProfiles,
                notifications,
                profiles,
                accounts,
                relationships,
                mock(MessageService.class),
                null,
                null);

        assertEquals(1, service.getMutualConnectionsCount("target-id", "viewer@example.com").get("count"));
        assertEquals(1, service.getProfileMutualConnections("target-id", "viewer@example.com").size());
        assertEquals(1, service.getProfileFollowers("target-id").size());
    }

    private static UserAccountEntity account(Long id, String email, String publicUserId) {
        UserAccountEntity account = new UserAccountEntity();
        account.setId(id);
        account.setEmail(email);
        account.setPublicUserId(publicUserId);
        account.setName(publicUserId);
        return account;
    }

    private static UserProfileRelationshipEntity relationship(String ownerId, String targetId) {
        UserProfileRelationshipEntity relationship = new UserProfileRelationshipEntity();
        relationship.setOwnerUserId(ownerId);
        relationship.setTargetProfileId(targetId);
        relationship.setConnected(true);
        relationship.setStatus("ACCEPTED");
        return relationship;
    }

    private static UserProfileRelationshipEntity following(String ownerId, String targetId) {
        UserProfileRelationshipEntity relationship = new UserProfileRelationshipEntity();
        relationship.setOwnerUserId(ownerId);
        relationship.setTargetProfileId(targetId);
        relationship.setFollowing(true);
        return relationship;
    }
}
