package com.truehunt.social.repository;

import com.truehunt.social.model.EndorsementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EndorsementRepository extends JpaRepository<EndorsementEntity, Long> {
    List<EndorsementEntity> findAllByReceiverProfileIdIgnoreCaseOrderByCreatedAtDesc(String receiverProfileId);

    Optional<EndorsementEntity> findByIdAndReceiverProfileIdIgnoreCase(Long id, String receiverProfileId);

    boolean existsByReceiverProfileIdIgnoreCaseAndEndorserProfileIdIgnoreCase(
            String receiverProfileId,
            String endorserProfileId
    );
}