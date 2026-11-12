package com.truehunt.social.repository;

import com.truehunt.social.model.OpportunityApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface OpportunityApplicationRepository extends JpaRepository<OpportunityApplicationEntity, Long> {
    boolean existsByPostIdAndApplicantIdentity(Long postId, String applicantIdentity);
    Optional<OpportunityApplicationEntity> findByPostIdAndApplicantIdentity(Long postId, String applicantIdentity);
    long countByPostId(Long postId);
    void deleteAllByPostIdIn(Collection<Long> postIds);
    void deleteAllByApplicantIdentityIn(Collection<String> applicantIdentities);
}
