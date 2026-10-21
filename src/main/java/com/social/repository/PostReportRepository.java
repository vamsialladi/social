package com.social.repository;

import com.social.entity.PostReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostReportRepository extends JpaRepository<PostReport, String> {
    List<PostReport> findByPostId(String postId);
    boolean existsByPostIdAndReporterUserId(String postId, String reporterUserId);
    long countByPostId(String postId);
}
