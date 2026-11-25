package com.truehunt.social.repository;

import com.truehunt.social.model.SavedPostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedPostRepository extends JpaRepository<SavedPostEntity, Long> {
    List<SavedPostEntity> findAllByUserEmailIgnoreCaseOrderBySavedAtDesc(String userEmail);
    Optional<SavedPostEntity> findByUserEmailIgnoreCaseAndPostId(String userEmail, Long postId);
    boolean existsByUserEmailIgnoreCaseAndPostId(String userEmail, Long postId);
    void deleteByUserEmailIgnoreCaseAndPostId(String userEmail, Long postId);
    void deleteByUserEmailIgnoreCase(String userEmail);
}
