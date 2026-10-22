package com.social.repository;

import com.social.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.id NOT IN :excludedIds ORDER BY u.createdAt DESC")
    List<User> findSuggestionsExcludingIds(@Param("excludedIds") Collection<String> excludedIds, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.id != :selfId ORDER BY u.createdAt DESC")
    List<User> findSuggestionsExcludingSelf(@Param("selfId") String selfId, Pageable pageable);
}
