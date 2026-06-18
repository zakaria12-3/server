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

    Optional<Application> findByCvPath(String cvPath);

    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);

    List<Application> findByJobIdAndStatus(Long jobId, String status);

    void deleteByCandidateId(Long candidateId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(a) FROM Application a WHERE a.candidate.id = :candidateId AND a.status IN ('FINAL_ACCEPTED', 'ACCEPTED') AND a.interviewDate >= :startOfDay AND a.interviewDate < :endOfDay AND a.id != :applicationId")
    long countConflictingInterviews(@org.springframework.data.repository.query.Param("candidateId") Long candidateId, 
                                    @org.springframework.data.repository.query.Param("startOfDay") java.time.LocalDateTime startOfDay, 
                                    @org.springframework.data.repository.query.Param("endOfDay") java.time.LocalDateTime endOfDay, 
                                    @org.springframework.data.repository.query.Param("applicationId") Long applicationId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(a) FROM Application a
        WHERE a.candidate.id = :candidateId
          AND a.interviewDate = :interviewDate
          AND a.id <> :applicationId
          AND a.status IN ('INTERVIEW_SCHEDULED', 'INTERVIEW_COMPLETED', 'FINAL_ACCEPTED', 'PRESELECTED_FOR_INTERVIEW', 'ACCEPTED')
    """)
    long countCandidateInterviewAtSameTime(@org.springframework.data.repository.query.Param("candidateId") Long candidateId,
                                           @org.springframework.data.repository.query.Param("interviewDate") java.time.LocalDateTime interviewDate,
                                           @org.springframework.data.repository.query.Param("applicationId") Long applicationId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(a) FROM Application a
        WHERE a.job.recruiter.id = :recruiterId
          AND a.interviewDate = :interviewDate
          AND a.id <> :applicationId
          AND a.status IN ('INTERVIEW_SCHEDULED', 'INTERVIEW_COMPLETED', 'FINAL_ACCEPTED', 'PRESELECTED_FOR_INTERVIEW', 'ACCEPTED')
    """)
    long countRecruiterInterviewAtSameTime(@org.springframework.data.repository.query.Param("recruiterId") Long recruiterId,
                                           @org.springframework.data.repository.query.Param("interviewDate") java.time.LocalDateTime interviewDate,
                                           @org.springframework.data.repository.query.Param("applicationId") Long applicationId);
}
