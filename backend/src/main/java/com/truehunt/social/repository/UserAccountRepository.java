package com.truehunt.social.repository;

import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.validation.PhoneNumberPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
        if (phoneNumber == null || phoneNumber.isBlank()) return Optional.empty();
        String trimmed = phoneNumber.trim();
        Optional<UserAccountEntity> direct = findFirstByPhoneNumber(trimmed);
        if (direct.isPresent()) return direct;
        String normalized = PhoneNumberPolicy.normalizeUnchecked(trimmed);
        if (normalized != null && !normalized.equals(trimmed)) {
            Optional<UserAccountEntity> byNorm = findFirstByPhoneNumber(normalized);
            if (byNorm.isPresent()) return byNorm;
        }
        String national = PhoneNumberPolicy.extractNationalNumber(trimmed);
        if (national != null && !national.equals(trimmed)) {
            Optional<UserAccountEntity> byNat = findFirstByPhoneNumber(national);
            if (byNat.isPresent()) return byNat;
        }
        return Optional.empty();
    }
    default Optional<UserAccountEntity> findByPhone(String phone) {
        return findByPhoneNumber(phone);
    }

    List<UserAccountEntity> findAllByEmail(String email);

    @Query("SELECT u FROM UserAccountEntity u WHERE u.phoneNumber = :phone OR u.phoneNumber = :normPhone")
    List<UserAccountEntity> findMatchingPhoneNumbers(@Param("phone") String phone, @Param("normPhone") String normPhone);

    default List<UserAccountEntity> findAllByPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) return List.of();
        String trimmed = phoneNumber.trim();
        String norm = PhoneNumberPolicy.normalizeUnchecked(trimmed);
        return findMatchingPhoneNumbers(trimmed, norm != null ? norm : trimmed);
    }

    List<UserAccountEntity> findAllByRoleIgnoreCase(String role);

    boolean existsByPublicUserId(String publicUserId);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT COUNT(u) > 0 FROM UserAccountEntity u WHERE u.phoneNumber = :phone OR u.phoneNumber = :normPhone")
    boolean existsByMatchingPhoneNumber(@Param("phone") String phone, @Param("normPhone") String normPhone);

    default boolean existsByPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) return false;
        String trimmed = phoneNumber.trim();
        String norm = PhoneNumberPolicy.normalizeUnchecked(trimmed);
        return existsByMatchingPhoneNumber(trimmed, norm != null ? norm : trimmed);
    }

    default boolean existsByPhone(String phone) {
        return existsByPhoneNumber(phone);
    }

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);
}
