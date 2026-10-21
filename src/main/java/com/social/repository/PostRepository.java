package com.social.repository;

import com.social.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {

    // Opportunities: type = 'opportunity', sorted newest first
    List<Post> findByTypeOrderByCreatedAtDesc(String type, Pageable pageable);

    List<Post> findByTypeAndCreatedAtLessThanOrderByCreatedAtDesc(String type, Instant createdAt, Pageable pageable);

    // Regular feed: exclude type = 'opportunity' (Rule: Opportunity posts must not appear in the ordinary posts feed)
    List<Post> findByTypeNotOrderByCreatedAtDesc(String type, Pageable pageable);

    List<Post> findByTypeNotAndCreatedAtLessThanOrderByCreatedAtDesc(String type, Instant createdAt, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.type = :type")
    long countByType(@Param("type") String type);
}
