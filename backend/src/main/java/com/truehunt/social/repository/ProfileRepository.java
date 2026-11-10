package com.truehunt.social.repository;

import com.truehunt.social.model.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {
    Optional<ProfileEntity> findFirstByUserIdIgnoreCase(String userId);
    default Optional<ProfileEntity> findByUserId(String userId) {
        if (userId == null) return Optional.empty();
        return findFirstByUserIdIgnoreCase(userId);
    }

    Optional<ProfileEntity> findFirstByEmailIgnoreCase(String email);
    default Optional<ProfileEntity> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return findFirstByEmailIgnoreCase(email);
    }

    Optional<ProfileEntity> findFirstByNameIgnoreCase(String name);
    default Optional<ProfileEntity> findByName(String name) {
        if (name == null) return Optional.empty();
        return findFirstByNameIgnoreCase(name);
    }

    List<ProfileEntity> findAllByEmailIgnoreCase(String email);
    default List<ProfileEntity> findAllByEmail(String email) {
        return findAllByEmailIgnoreCase(email);
    }

    List<ProfileEntity> findAllByUserIdIgnoreCase(String userId);
    default List<ProfileEntity> findAllByUserId(String userId) {
        return findAllByUserIdIgnoreCase(userId);
    }
}
