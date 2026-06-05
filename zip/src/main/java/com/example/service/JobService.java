package com.example.service;


import com.example.dto.JobCandidateDto;
import com.example.model.Job;
import com.example.model.User;
import com.example.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
public class JobService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobRepository;

    private final QuizResultRepository quizResultRepository;

    private final QuizRepository quizRepository;


    private final UserRepository userRepository;


    private final ApplicationRepository applicationRepository;

    private final EmailService emailService;
    private final ModerationService moderationService;


    public List<Job> getJobsByRecruiter(Long recruiterId) {
        return jobRepository.findByRecruiterIdOrderByCreatedAtDesc(recruiterId);
    }


    public JobService(JobRepository jobRepository, QuizResultRepository quizResultRepository, QuizRepository quizRepository, UserRepository userRepository, ApplicationRepository applicationRepository, EmailService emailService, ModerationService moderationService) {
        this.jobRepository = jobRepository;
        this.quizResultRepository = quizResultRepository;
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.emailService = emailService;
        this.moderationService = moderationService;
    }

    public Job createJob(Job job, User recruiter) {
        ensureRecruiterCanManageJobs(recruiter);
        validateExpirationDate(job.getExpirationDate());
        assignRecruiterCompany(job, recruiter);
        job.setRecruiter(recruiter);
        job.setCreatedAt(LocalDateTime.now());
        job.setActive(true);
        moderationService.evaluateJob(job);
        Job savedJob = jobRepository.save(job);

        java.util.concurrent.CompletableFuture.runAsync(() -> notifyCandidatesAboutNewJob(savedJob));

        return savedJob;
    }

    private void notifyCandidatesAboutNewJob(Job savedJob) {
        List<User> candidates = userRepository.findByRoleAndEnabledTrue(com.example.model.Role.ROLE_CANDIDATE);
        int notified = 0;
        for (User candidate : candidates) {
            if (candidate.getEmail() == null || candidate.getEmail().isBlank()) {
                continue;
            }
            emailService.sendJobPostedEmail(candidate.getEmail(), savedJob.getTitle(), savedJob.getCompany(), savedJob.getDescription());
            notified++;
        }
        LOGGER.info("Sent new job listing notification for job {} to {} candidates", savedJob.getId(), notified);
    }

    public Job updateJob(Long jobId, Job updates, User recruiter) {
        ensureRecruiterCanManageJobs(recruiter);
        Job job = getById(jobId);
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new RuntimeException("Not allowed to edit this job");
        }

        validateExpirationDate(updates.getExpirationDate());
        job.setTitle(updates.getTitle());
        job.setDescription(updates.getDescription());
        job.setLocation(updates.getLocation());
        assignRecruiterCompany(job, recruiter);
        job.setExpirationDate(updates.getExpirationDate());
        job.setActive(!isExpired(updates.getExpirationDate()));
        moderationService.evaluateJob(job);
        return jobRepository.save(job);
    }

    public Job getRecruiterJob(Long jobId, User recruiter) {
        ensureRecruiterCanManageJobs(recruiter);
        Job job = getById(jobId);
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new RuntimeException("Not allowed to view this job");
        }
        return job;
    }

    @Transactional
    public void deleteRecruiterJob(Long jobId, User recruiter) {
        getRecruiterJob(jobId, recruiter);
        deleteJob(jobId);
    }

    public void setJobsActiveByRecruiter(Long recruiterId, boolean active) {
        List<Job> recruiterJobs = getJobsByRecruiter(recruiterId);
        for (Job job : recruiterJobs) {
            job.setActive(active && !isExpired(job.getExpirationDate()));
        }
        jobRepository.saveAll(recruiterJobs);
    }

    public List<JobCandidateDto> getJobsForCandidate(String email) {

        User candidate = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Job> jobs = jobRepository.findVisibleJobs(LocalDate.now());

        return jobs.stream().map(job -> {

            JobCandidateDto dto = new JobCandidateDto();
            dto.setId(job.getId());
            dto.setTitle(job.getTitle());
            dto.setCompany(job.getCompany());
            dto.setLocation(job.getLocation());
            dto.setDescription(job.getDescription());
            dto.setCreatedAt(job.getCreatedAt());
            dto.setExpirationDate(job.getExpirationDate());
            boolean applied = applicationRepository
                    .existsByJobIdAndCandidateId(job.getId(), candidate.getId());

            dto.setApplied(applied);
            dto.setSuspicious(job.isSuspicious());

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
        return jobRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    public Job getById(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
    }

    public boolean isAvailable(Job job) {
        return job.getActive()
                && !isExpired(job.getExpirationDate())
                && job.getRecruiter() != null
                && job.getRecruiter().isEnabled()
                && "APPROVED".equalsIgnoreCase(job.getModerationStatus());
    }

    private boolean isExpired(LocalDate expirationDate) {
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }

    private void validateExpirationDate(LocalDate expirationDate) {
        if (expirationDate == null) {
            throw new RuntimeException("Expiration date is required");
        }
        if (expirationDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("Expiration date cannot be in the past");
        }
    }

    private void assignRecruiterCompany(Job job, User recruiter) {
        if (recruiter.getCompany() == null || recruiter.getCompany().getName() == null || recruiter.getCompany().getName().isBlank()) {
            throw new RuntimeException("Recruiter must belong to an approved company before posting jobs");
        }
        job.setCompany(recruiter.getCompany().getName());
    }

    public void ensureRecruiterCanManageJobs(User recruiter) {
        if (recruiter == null || !recruiter.isEmailVerified() || !recruiter.isEnabled()
                || "PENDING".equalsIgnoreCase(recruiter.getApprovalStatus())
                || "REJECTED".equalsIgnoreCase(recruiter.getApprovalStatus())) {
            throw new RuntimeException("Recruiter account must be approved before managing jobs");
        }
    }

    public Job approveJob(Long jobId) {
        Job job = getById(jobId);
        moderationService.approveJob(job);
        job.setActive(!isExpired(job.getExpirationDate()));
        return jobRepository.save(job);
    }

    public Job blockJob(Long jobId, String reason) {
        Job job = getById(jobId);
        moderationService.rejectJob(job, reason);
        return jobRepository.save(job);
    }

    public Job rescanJob(Long jobId) {
        Job job = getById(jobId);
        moderationService.evaluateJob(job);
        if ("BLOCKED".equalsIgnoreCase(job.getModerationStatus())) {
            job.setActive(false);
        }
        return jobRepository.save(job);
    }
}
