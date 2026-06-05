package com.example.repository;

import com.example.model.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    void deleteByJobId(Long jobId);
    void deleteByCandidateId(Long candidateId);
    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);
    long countByCandidateIdAndCheatingSuspectedTrue(Long candidateId);
}
