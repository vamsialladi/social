package com.social.repository;

import com.social.entity.Post;
import com.social.entity.PostType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {

    @Query("SELECT p FROM Post p WHERE LOWER(p.type) <> LOWER(:excludeType) " +
           "AND (p.isSpam = false OR p.isSpam IS NULL) " +
           "AND (p.visibility = com.social.entity.PostVisibility.PUBLIC OR (:viewerId IS NOT NULL AND p.authorId = :viewerId)) " +
           "AND (:cursorTime IS NULL OR p.createdAt < :cursorTime) " +
           "ORDER BY p.createdAt DESC")
    List<Post> findFeedPosts(
            @Param("excludeType") String excludeType,
            @Param("viewerId") String viewerId,
            @Param("cursorTime") Instant cursorTime,
            Pageable pageable
    );

    default List<Post> findFeedPosts(
            PostType excludeType,
            String viewerId,
            Instant cursorTime,
            Pageable pageable
    ) {
        return findFeedPosts(excludeType != null ? excludeType.toValue() : "opportunity", viewerId, cursorTime, pageable);
    }

    List<Post> findByAuthorIdOrderByCreatedAtDesc(String authorId);

    // Opportunities: type = 'opportunity', sorted newest first
    List<Post> findByTypeOrderByCreatedAtDesc(String type, Pageable pageable);

    List<Post> findByTypeAndCreatedAtLessThanOrderByCreatedAtDesc(String type, Instant createdAt, Pageable pageable);

    // Regular feed: exclude type = 'opportunity'
    List<Post> findByTypeNotOrderByCreatedAtDesc(String type, Pageable pageable);

    List<Post> findByTypeNotAndCreatedAtLessThanOrderByCreatedAtDesc(String type, Instant createdAt, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Post p WHERE LOWER(p.type) = LOWER(:type)")
    long countByType(@Param("type") String type);
}
