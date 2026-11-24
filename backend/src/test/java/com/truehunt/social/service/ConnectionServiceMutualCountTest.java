package com.truehunt.social.service;

import com.truehunt.social.model.UserProfileRelationshipEntity;
import com.truehunt.social.repository.NetworkProfileRepository;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import com.truehunt.social.repository.UserProfileRelationshipRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConnectionServiceMutualCountTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void countsOnlyAcceptedConnectionsSharedByViewerAndTarget() {
        NetworkProfileRepository networkProfiles = mock(NetworkProfileRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        ProfileRepository profiles = mock(ProfileRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserProfileRelationshipRepository relationships = mock(UserProfileRelationshipRepository.class);
        UserProfileRelationshipEntity follower = relationship("follower-id", "target-id", "NONE");
        follower.setFollowing(true);
        when(relationships.findAll()).thenReturn(List.of(
                relationship("viewer-id", "shared-id", "ACCEPTED"),
                relationship("target-id", "shared-id", "ACCEPTED"),
                relationship("viewer-id", "viewer-only-id", "ACCEPTED"),
                relationship("target-id", "pending-only-id", "PENDING"),
                follower
        ));

        ConnectionService service = new ConnectionService(
                networkProfiles,
                notifications,
                profiles,
                accounts,
                relationships,
                mock(MessageService.class),
                null,
                null);

        assertEquals(1, service.getMutualConnectionsCount("viewer-id", "target-id"));
        assertEquals(1, service.getProfileMutualConnections("viewer-id", "target-id").size());
        assertEquals(1, service.getProfileFollowers("target-id").size());
    }

    private UserProfileRelationshipEntity relationship(String ownerId, String targetId, String status) {
        UserProfileRelationshipEntity relationship = new UserProfileRelationshipEntity();
        relationship.setOwnerUserId(ownerId);
        relationship.setTargetProfileId(targetId);
        relationship.setStatus(status);
        return relationship;
    }
}
