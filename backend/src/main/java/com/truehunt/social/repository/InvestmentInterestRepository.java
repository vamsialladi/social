package com.truehunt.social.repository;

import com.truehunt.social.model.InvestmentInterestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestmentInterestRepository extends JpaRepository<InvestmentInterestEntity, String> {
    List<InvestmentInterestEntity> findAllByOrderBySubmittedAtDesc();
}

