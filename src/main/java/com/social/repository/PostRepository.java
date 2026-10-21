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

    @Query("SELECT p FROM Post p WHERE p.type <> :excludeType " +
           "AND p.isSpam = false " +
           "AND (p.visibility = com.social.entity.PostVisibility.PUBLIC OR (:viewerId IS NOT NULL AND p.authorId = :viewerId)) " +
           "AND (:cursorTime IS NULL OR p.createdAt < :cursorTime) " +
           "ORDER BY p.createdAt DESC")
    List<Post> findFeedPosts(
            @Param("excludeType") PostType excludeType,
            @Param("viewerId") String viewerId,
            @Param("cursorTime") Instant cursorTime,
            Pageable pageable
    );

    List<Post> findByAuthorIdOrderByCreatedAtDesc(String authorId);
}
