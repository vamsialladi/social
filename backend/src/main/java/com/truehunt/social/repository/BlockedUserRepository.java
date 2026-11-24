package com.truehunt.social.repository;

import com.truehunt.social.model.BlockedUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUserEntity, Long> {
    List<BlockedUserEntity> findAllByBlockerEmailIgnoreCaseOrderByCreatedAtDesc(String blockerEmail);
    Optional<BlockedUserEntity> findByBlockerEmailIgnoreCaseAndBlockedIdentityIgnoreCase(String blockerEmail, String blockedIdentity);
    Optional<BlockedUserEntity> findByBlockerEmailIgnoreCaseAndBlockedEmailIgnoreCase(String blockerEmail, String blockedEmail);
    void deleteByBlockerEmailIgnoreCaseAndBlockedIdentityIgnoreCase(String blockerEmail, String blockedIdentity);
    void deleteByBlockerEmailIgnoreCase(String blockerEmail);
}
