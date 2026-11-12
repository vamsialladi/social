package com.truehunt.social.repository;

import com.truehunt.social.model.JobApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplicationEntity, String> {
    List<JobApplicationEntity> findAllByOrderBySubmittedAtDesc();
}

