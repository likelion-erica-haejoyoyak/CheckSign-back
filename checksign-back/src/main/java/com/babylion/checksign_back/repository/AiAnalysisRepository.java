package com.babylion.checksign_back.repository;

import com.babylion.checksign_back.model.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, String> {
} 