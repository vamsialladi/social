package com.truehunt.social.repository;

import com.truehunt.social.model.EndorsementRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EndorsementRequestRepository extends JpaRepository<EndorsementRequestEntity, Long> {

    Optional<EndorsementRequestEntity> findBySenderProfileIdIgnoreCaseAndRecipientProfileIdIgnoreCase(
            String senderProfileId,
            String recipientProfileId
    );

    List<EndorsementRequestEntity> findAllBySenderProfileIdIgnoreCase(String senderProfileId);
}
