package com.truehunt.social.repository;

import com.truehunt.social.model.OrganizationPageAnalyticsEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrganizationPageAnalyticsEventRepository
        extends JpaRepository<OrganizationPageAnalyticsEventEntity, Long> {

    List<OrganizationPageAnalyticsEventEntity> findAllByGroupIdAndCreatedAtAfterOrderByCreatedAtAsc(
            Long groupId,
            LocalDateTime createdAt);

    boolean existsByGroupIdAndActorIdAndActivityTypeAndCreatedAtAfter(
            Long groupId,
            String actorId,
            String activityType,
            LocalDateTime createdAt);

    void deleteAllByGroupIdAndActorIdAndActivityType(Long groupId, String actorId, String activityType);
}
