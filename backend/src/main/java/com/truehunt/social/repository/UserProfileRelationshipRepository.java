package com.truehunt.social.repository;

import com.truehunt.social.model.UserProfileRelationshipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRelationshipRepository extends JpaRepository<UserProfileRelationshipEntity, Long> {
    Optional<UserProfileRelationshipEntity> findByOwnerUserIdAndTargetProfileId(
            String ownerUserId,
            String targetProfileId
    );

    List<UserProfileRelationshipEntity> findAllByOwnerUserIdAndConnectedTrue(String ownerUserId);

    List<UserProfileRelationshipEntity> findAllByOwnerUserIdAndFollowingTrue(String ownerUserId);

    List<UserProfileRelationshipEntity> findAllByOwnerUserIdOrTargetProfileId(
            String ownerUserId,
            String targetProfileId
    );
}
