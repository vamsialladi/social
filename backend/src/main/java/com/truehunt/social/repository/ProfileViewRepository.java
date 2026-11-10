package com.truehunt.social.repository;

import com.truehunt.social.model.ProfileViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileViewRepository extends JpaRepository<ProfileViewEntity, Long> {
    Optional<ProfileViewEntity> findFirstByOwnerUserIdAndViewerUserId(String ownerUserId, String viewerUserId);

    default Optional<ProfileViewEntity> findByOwnerUserIdAndViewerUserId(String ownerUserId, String viewerUserId) {
        return findFirstByOwnerUserIdAndViewerUserId(ownerUserId, viewerUserId);
    }

    List<ProfileViewEntity> findAllByOwnerUserIdOrderByViewedAtDesc(String ownerUserId);
    List<ProfileViewEntity> findAllByOwnerUserIdOrViewerUserId(String ownerUserId, String viewerUserId);
}
