package com.truehunt.social.repository;

import com.truehunt.social.model.PostReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostReportRepository extends JpaRepository<PostReportEntity, Long> {
    List<PostReportEntity> findAllByReporterEmailIgnoreCaseOrderByReportedAtDesc(String reporterEmail);
    List<PostReportEntity> findAllByOrderByReportedAtDesc();
}
