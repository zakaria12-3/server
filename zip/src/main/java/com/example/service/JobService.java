package com.example.service;


import com.example.dto.JobCandidateDto;
import com.example.model.Job;
import com.example.model.User;
import com.example.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;

    private final QuizResultRepository quizResultRepository;

    private final QuizRepository quizRepository;


    private final UserRepository userRepository;


    private final ApplicationRepository applicationRepository;


    public List<Job> getJobsByRecruiter(Long recruiterId) {
        return jobRepository.findByRecruiterId(recruiterId);
    }


    public JobService(JobRepository jobRepository, QuizResultRepository quizResultRepository, QuizRepository quizRepository, UserRepository userRepository, ApplicationRepository applicationRepository) {
        this.jobRepository = jobRepository;
        this.quizResultRepository = quizResultRepository;
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
    }

    public Job createJob(Job job, User recruiter) {
        job.setRecruiter(recruiter);
        job.setCreatedAt(LocalDateTime.now());
        return jobRepository.save(job);
    }public List<JobCandidateDto> getJobsForCandidate(String email) {

        User candidate = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Job> jobs = jobRepository.findAll();

        return jobs.stream().map(job -> {

            JobCandidateDto dto = new JobCandidateDto();
            dto.setId(job.getId());
            dto.setTitle(job.getTitle());
            dto.setCompany(job.getCompany());
            dto.setLocation(job.getLocation());
            dto.setDescription(job.getDescription());
            boolean applied = applicationRepository
                    .existsByJobIdAndCandidateId(job.getId(), candidate.getId());

            dto.setApplied(applied);

            return dto;

        }).toList();
    }



    @Transactional
    public void deleteJob(Long jobId) {
        quizResultRepository.deleteByJobId(jobId);
        applicationRepository.deleteByJobId(jobId);
        quizRepository.deleteByJobId(jobId);
        jobRepository.deleteById(jobId);
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Job getById(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
    }
}