package com.truehunt.social.repository;

import com.truehunt.social.model.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {
    Optional<UserAccountEntity> findFirstByEmailIgnoreCase(String email);
    default Optional<UserAccountEntity> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return findFirstByEmailIgnoreCase(email.trim());
    }

    Optional<UserAccountEntity> findFirstByPublicUserIdIgnoreCase(String publicUserId);
    default Optional<UserAccountEntity> findByPublicUserId(String publicUserId) {
        if (publicUserId == null) return Optional.empty();
        return findFirstByPublicUserIdIgnoreCase(publicUserId.trim());
    }

    Optional<UserAccountEntity> findFirstByNameIgnoreCase(String name);
    default Optional<UserAccountEntity> findByName(String name) {
        return findFirstByNameIgnoreCase(name);
    }

    Optional<UserAccountEntity> findFirstByPhoneNumber(String phoneNumber);
    default Optional<UserAccountEntity> findByPhoneNumber(String phoneNumber) {
        return findFirstByPhoneNumber(phoneNumber);
    }
    default Optional<UserAccountEntity> findByPhone(String phone) {
        return findFirstByPhoneNumber(phone);
    }

    List<UserAccountEntity> findAllByEmail(String email);
    List<UserAccountEntity> findAllByPhoneNumber(String phoneNumber);
    List<UserAccountEntity> findAllByRoleIgnoreCase(String role);

    boolean existsByPublicUserId(String publicUserId);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
}
