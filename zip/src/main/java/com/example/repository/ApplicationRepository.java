package com.example.repository;

import com.example.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByJobId(Long jobId);

    List<Application> findByCandidateId(Long candidateId);

    void deleteByJobId(Long jobId);
    Optional<Application> findByJobIdAndCandidateId(Long jobId, Long candidateId);

    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);

    List<Application> findByJobIdAndStatus(Long jobId, String status);

    void deleteByCandidateId(Long candidateId);
}