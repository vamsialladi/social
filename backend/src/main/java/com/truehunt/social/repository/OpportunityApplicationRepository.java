package com.truehunt.social.repository;

import com.truehunt.social.model.OpportunityApplicationEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OpportunityApplicationRepository extends JpaRepository<OpportunityApplicationEntity, Long> {
    boolean existsByPostIdAndApplicantIdentity(Long postId, String applicantIdentity);
    Optional<OpportunityApplicationEntity> findByPostIdAndApplicantIdentity(Long postId, String applicantIdentity);
    Optional<OpportunityApplicationEntity> findByIdAndPostId(Long id, Long postId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select application from OpportunityApplicationEntity application where application.id = :id and application.postId = :postId")
    Optional<OpportunityApplicationEntity> findByIdAndPostIdForUpdate(
            @Param("id") Long id,
            @Param("postId") Long postId
    );
    List<OpportunityApplicationEntity> findAllByPostIdOrderByAppliedAtDesc(Long postId);
    long countByPostId(Long postId);
    void deleteAllByPostIdIn(Collection<Long> postIds);
    void deleteAllByApplicantIdentityIn(Collection<String> applicantIdentities);
}
