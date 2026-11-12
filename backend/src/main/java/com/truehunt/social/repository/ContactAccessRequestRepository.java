package com.truehunt.social.repository;

import com.truehunt.social.model.ContactAccessRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactAccessRequestRepository extends JpaRepository<ContactAccessRequestEntity, Long> {
    Optional<ContactAccessRequestEntity> findFirstByRequesterProfileIdIgnoreCaseAndOwnerProfileIdIgnoreCaseOrderByCreatedAtDesc(
            String requesterProfileId,
            String ownerProfileId
    );

    List<ContactAccessRequestEntity> findAllByOwnerProfileIdIgnoreCaseAndStatusOrderByCreatedAtDesc(
            String ownerProfileId,
            String status
    );

    List<ContactAccessRequestEntity> findAllByRequesterProfileIdIgnoreCaseOrderByCreatedAtDesc(String requesterProfileId);
}
