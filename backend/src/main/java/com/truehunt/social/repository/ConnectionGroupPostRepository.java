package com.truehunt.social.repository;

import com.truehunt.social.model.ConnectionGroupPostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionGroupPostRepository extends JpaRepository<ConnectionGroupPostEntity, Long> {
    List<ConnectionGroupPostEntity> findAllByGroupIdOrderByCreatedAtDesc(Long groupId);
    Optional<ConnectionGroupPostEntity> findByIdAndGroupId(Long id, Long groupId);
    void deleteAllByGroupId(Long groupId);
}
