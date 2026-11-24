package com.truehunt.social.repository;

import com.truehunt.social.model.ConnectionGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionGroupRepository extends JpaRepository<ConnectionGroupEntity, Long> {
    List<ConnectionGroupEntity> findAllByOwnerIdOrderByCreatedAtDesc(String ownerId);
    List<ConnectionGroupEntity> findAllByOrderByCreatedAtDesc();
    Optional<ConnectionGroupEntity> findByIdAndOwnerId(Long id, String ownerId);
}
