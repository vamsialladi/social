package com.truehunt.social.repository;

import com.truehunt.social.model.InvestorAccessRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestorAccessRequestRepository extends JpaRepository<InvestorAccessRequestEntity, Long> {
    Optional<InvestorAccessRequestEntity> findFirstByApplicantEmailIgnoreCaseOrderByCreatedAtDesc(String email);
    Optional<InvestorAccessRequestEntity> findFirstByFeedPostIdAndApplicantEmailIgnoreCase(Long feedPostId, String email);
    List<InvestorAccessRequestEntity> findAllByOrderByCreatedAtDesc();
    List<InvestorAccessRequestEntity> findAllByStatusOrderByCreatedAtDesc(String status);
}
