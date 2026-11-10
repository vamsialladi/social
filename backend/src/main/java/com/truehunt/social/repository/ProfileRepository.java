package com.truehunt.social.repository;

import com.truehunt.social.model.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {
    Optional<ProfileEntity> findFirstByUserId(String userId);
    default Optional<ProfileEntity> findByUserId(String userId) {
        return findFirstByUserId(userId);
    }

    Optional<ProfileEntity> findFirstByEmail(String email);
    default Optional<ProfileEntity> findByEmail(String email) {
        return findFirstByEmail(email);
    }

    Optional<ProfileEntity> findFirstByNameIgnoreCase(String name);
    default Optional<ProfileEntity> findByName(String name) {
        return findFirstByNameIgnoreCase(name);
    }

    List<ProfileEntity> findAllByEmail(String email);
    List<ProfileEntity> findAllByUserId(String userId);
}
