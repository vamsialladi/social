package com.truehunt.repository;

import com.truehunt.model.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {
    List<Post> findByTypeInAndCreatedAtBeforeOrderByCreatedAtDesc(List<String> types, Instant cursor, Pageable pageable);
    List<Post> findByTypeInOrderByCreatedAtDesc(List<String> types, Pageable pageable);
    List<Post> findByTypeAndCreatedAtBeforeOrderByCreatedAtDesc(String type, Instant cursor, Pageable pageable);
    List<Post> findByTypeOrderByCreatedAtDesc(String type, Pageable pageable);
}
