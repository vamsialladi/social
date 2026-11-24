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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectionServiceRelationshipAliasTest {

    @Test
    void unfollowUsesExistingRelationshipAcrossUserIdAliases() {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserProfileRelationshipRepository relationships = mock(UserProfileRelationshipRepository.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        UserAccountEntity target = account(2L, "target@example.com", "target-id");
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(accounts.findByPublicUserId("owner-id")).thenReturn(Optional.of(owner));
        when(accounts.findByPublicUserId("target-id")).thenReturn(Optional.of(target));

        UserProfileRelationshipEntity existing = relationship("1", "2", true);
        when(relationships.findAll()).thenReturn(List.of(existing));
        ConnectionService service = service(accounts, relationships);

        service.toggleFollow("target-id", "owner@example.com");

        assertFalse(existing.isFollowing());
        assertEquals("1", existing.getOwnerUserId());
        assertEquals("2", existing.getTargetProfileId());
        verify(relationships).save(existing);
    }

    @Test
    void cancellingRequestClearsAllAliasRowsWithoutChangingTheirKeys() {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserProfileRelationshipRepository relationships = mock(UserProfileRelationshipRepository.class);
        UserAccountEntity owner = account(1L, "owner@example.com", "owner-id");
        UserAccountEntity target = account(2L, "target@example.com", "target-id");
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(accounts.findByPublicUserId("owner-id")).thenReturn(Optional.of(owner));
        when(accounts.findByPublicUserId("target-id")).thenReturn(Optional.of(target));

        UserProfileRelationshipEntity publicIds = relationship("owner-id", "target-id", false);
        publicIds.setStatus("PENDING");
        publicIds.setSenderUserId("owner-id");
        publicIds.setConnectionNote("Let's connect");
        UserProfileRelationshipEntity accountIds = relationship("1", "2", false);
        accountIds.setStatus("PENDING");
        accountIds.setSenderUserId("1");
        when(relationships.findAll()).thenReturn(List.of(publicIds, accountIds));
        ConnectionService service = service(accounts, relationships);

        service.cancelConnectionRequest("target-id", "owner@example.com");

        assertCancelled(publicIds, "owner-id", "target-id");
        assertCancelled(accountIds, "1", "2");
        verify(relationships).save(publicIds);
        verify(relationships).save(accountIds);
    }

    private static ConnectionService service(
            UserAccountRepository accounts,
            UserProfileRelationshipRepository relationships) {
        return new ConnectionService(
                mock(NetworkProfileRepository.class),
                mock(NotificationRepository.class),
                mock(ProfileRepository.class),
                accounts,
                relationships,
                mock(MessageService.class),
                null,
                null);
    }

    private static UserAccountEntity account(Long id, String email, String publicUserId) {
        UserAccountEntity account = new UserAccountEntity();
        account.setId(id);
        account.setEmail(email);
        account.setPublicUserId(publicUserId);
        return account;
    }

    private static UserProfileRelationshipEntity relationship(String ownerId, String targetId, boolean following) {
        UserProfileRelationshipEntity relationship = new UserProfileRelationshipEntity();
        relationship.setOwnerUserId(ownerId);
        relationship.setTargetProfileId(targetId);
        relationship.setFollowing(following);
        return relationship;
    }

    private static void assertCancelled(
            UserProfileRelationshipEntity relationship,
            String expectedOwnerId,
            String expectedTargetId) {
        assertEquals(expectedOwnerId, relationship.getOwnerUserId());
        assertEquals(expectedTargetId, relationship.getTargetProfileId());
        assertFalse(relationship.isConnected());
        assertNull(relationship.getStatus());
        assertNull(relationship.getSenderUserId());
        assertNull(relationship.getConnectionNote());
    }
}
