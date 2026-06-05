package com.example.repository;

import com.example.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByRecruiterIdOrderByCreatedAtDesc(Long recruiterId);
    
    List<Job> findByTitleContainingIgnoreCaseOrCompanyContainingIgnoreCase(String title, String company);

    long countByTitleAndDescription(String title, String description);

    @Query("""
            select j from Job j
            where (j.active is null or j.active = true)
              and (j.expirationDate is null or j.expirationDate >= :today)
              and j.recruiter.enabled = true
              and (j.moderationStatus is null or j.moderationStatus = 'APPROVED')
            order by j.createdAt desc
            """)
    List<Job> findVisibleJobs(@Param("today") LocalDate today);

    @Query("""
            select j from Job j
            where (lower(j.title) like lower(concat('%', :query, '%'))
                or lower(j.company) like lower(concat('%', :query, '%')))
              and (j.active is null or j.active = true)
              and (j.expirationDate is null or j.expirationDate >= :today)
              and j.recruiter.enabled = true
              and (j.moderationStatus is null or j.moderationStatus = 'APPROVED')
            order by j.createdAt desc
            """)
    List<Job> searchVisibleJobs(@Param("query") String query, @Param("today") LocalDate today);


}
