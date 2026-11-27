package com.truehunt.social.repository;

import com.truehunt.social.model.OrganizationPageSubscriptionRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationPageSubscriptionRequestRepository
        extends JpaRepository<OrganizationPageSubscriptionRequestEntity, Long> {

    boolean existsByPageIdAndRequesterIdInAndStatusIgnoreCase(
            Long pageId,
            Collection<String> requesterIds,
            String status);

    Optional<OrganizationPageSubscriptionRequestEntity> findFirstByPageIdAndRequesterIdAndStatusIgnoreCaseOrderByCreatedAtDesc(
            Long pageId,
            String requesterId,
            String status);

    List<OrganizationPageSubscriptionRequestEntity> findAllByPageIdInAndStatusIgnoreCaseOrderByCreatedAtDesc(
            Collection<Long> pageIds,
            String status);

    List<OrganizationPageSubscriptionRequestEntity> findAllByPageIdAndStatusIgnoreCaseOrderByCreatedAtDesc(
            Long pageId,
            String status);

    void deleteAllByPageIdIn(Collection<Long> pageIds);
}
