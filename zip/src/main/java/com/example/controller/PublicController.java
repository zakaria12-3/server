package com.example.controller;

import com.example.dto.JobCandidateDto;
import com.example.model.Job;
import com.example.repository.JobRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public")
public class PublicController {

    private final JobRepository jobRepository;

    public PublicController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        List<JobCandidateDto> jobs = jobRepository.findVisibleJobs(LocalDate.now())
                .stream()
                .map(this::toJobDto)
                .toList();

        return Map.of(
                "totalJobs", jobs.size(),
                "recentJobs", jobs.stream().limit(5).toList()
        );
    }

    @GetMapping("/jobs")
    public List<JobCandidateDto> jobs() {
        return jobRepository.findVisibleJobs(LocalDate.now())
                .stream()
                .map(this::toJobDto)
                .toList();
    }

    private JobCandidateDto toJobDto(Job job) {
        JobCandidateDto dto = new JobCandidateDto();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setCompany(job.getCompany());
        dto.setLocation(job.getLocation());
        dto.setDescription(job.getDescription());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setExpirationDate(job.getExpirationDate());
        dto.setApplied(false);
        return dto;
    }
}
