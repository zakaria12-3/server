package com.example.controller;

import com.example.dto.PlatformRatingSummaryDto;
import com.example.dto.JobCandidateDto;
import com.example.model.Job;
import com.example.repository.JobRepository;
import com.example.service.PlatformRatingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public")
public class PublicController {

    private final JobRepository jobRepository;
    private final PlatformRatingService platformRatingService;

    public PublicController(JobRepository jobRepository, PlatformRatingService platformRatingService) {
        this.jobRepository = jobRepository;
        this.platformRatingService = platformRatingService;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        List<JobCandidateDto> jobs = jobRepository.findVisibleJobs(LocalDate.now())
                .stream()
                .map(this::toJobDto)
                .toList();
        PlatformRatingSummaryDto ratings = platformRatingService.getSummary();

        return Map.of(
                "totalJobs", jobs.size(),
                "activeCompanies", jobs.stream()
                        .map(JobCandidateDto::getCompany)
                        .filter(company -> company != null && !company.isBlank())
                        .distinct()
                        .count(),
                "recentJobs", jobs.stream().limit(5).toList(),
                "averageRating", ratings.getAverageRating(),
                "ratingCount", ratings.getRatingCount()
        );
    }

    @GetMapping("/hero-slides")
    public List<Map<String, String>> heroSlides() {
        return List.of(
                Map.of(
                        "role", "CANDIDATE",
                        "title", "Parcours candidat",
                        "subtitle", "Une candidate consulte des offres verifiees",
                        "imageUrl", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=900&q=80"
                ),
                Map.of(
                        "role", "RECRUTEUR",
                        "title", "Espace recruteur",
                        "subtitle", "Un recruteur compare les profils et prepare les entretiens",
                        "imageUrl", "https://images.unsplash.com/photo-1551836022-d5d88e9218df?auto=format&fit=crop&w=900&q=80"
                ),
                Map.of(
                        "role", "EQUIPE",
                        "title", "Collaboration RH",
                        "subtitle", "Des equipes prennent de meilleures decisions ensemble",
                        "imageUrl", "https://images.unsplash.com/photo-1521737604893-d14cc237f11d?auto=format&fit=crop&w=900&q=80"
                )
        );
    }

    @GetMapping("/jobs")
    public List<JobCandidateDto> jobs() {
        return jobRepository.findVisibleJobs(LocalDate.now())
                .stream()
                .map(this::toJobDto)
                .toList();
    }

    @GetMapping("/jobs/{id}")
    public JobCandidateDto job(@PathVariable Long id) {
        Job job = jobRepository.findById(id)
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .filter(candidate -> candidate.getExpirationDate() == null || !candidate.getExpirationDate().isBefore(LocalDate.now()))
                .filter(candidate -> candidate.getRecruiter() != null && candidate.getRecruiter().isEnabled())
                .filter(candidate -> candidate.getModerationStatus() == null || "APPROVED".equalsIgnoreCase(candidate.getModerationStatus()))
                .orElseThrow(() -> new RuntimeException("Job not found"));

        return toJobDto(job);
    }

    private JobCandidateDto toJobDto(Job job) {
        JobCandidateDto dto = new JobCandidateDto();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setCompany(job.getCompany());
        dto.setCategory(job.getCategory());
        dto.setLocation(job.getLocation());
        dto.setDescription(job.getDescription());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setExpirationDate(job.getExpirationDate());
        if (job.getRecruiter() != null) {
            dto.setRecruiterName(job.getRecruiter().getRealUsername());
            dto.setRecruiterTitle(job.getRecruiter().getJobTitle());
            dto.setRecruiterEmail(job.getRecruiter().getEmail());
        }
        dto.setApplied(false);
        return dto;
    }
}
