package com.truehunt.social.repository;

import com.truehunt.social.model.HiddenContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HiddenContentRepository extends JpaRepository<HiddenContentEntity, Long> {
    List<HiddenContentEntity> findAllByUserEmailIgnoreCase(String userEmail);
    List<HiddenContentEntity> findAllByUserEmailIgnoreCaseAndTargetTypeIgnoreCase(String userEmail, String targetType);
    Optional<HiddenContentEntity> findByUserEmailIgnoreCaseAndTargetTypeIgnoreCaseAndTargetIdIgnoreCase(
            String userEmail, String targetType, String targetId
    );
    void deleteByUserEmailIgnoreCaseAndTargetTypeIgnoreCaseAndTargetIdIgnoreCase(
            String userEmail, String targetType, String targetId
    );
    void deleteByUserEmailIgnoreCase(String userEmail);
}
