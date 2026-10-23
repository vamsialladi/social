package com.truehunt.repository;

import com.truehunt.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, String> {
    Optional<Profile> findByUserId(String userId);
    boolean existsByUserId(String userId);
    List<Profile> findByUserIdNot(String userId);
    List<Profile> findByUserIdIn(List<String> userIds);
}
