package com.example.service;

import com.example.dto.CreateReportDto;
import com.example.dto.ReportDto;
import com.example.model.*;
import com.example.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final PostRepository postRepository;
    private final MessageRepository messageRepository;
    private final ModerationService moderationService;

    public ReportService(ReportRepository reportRepository,
                         UserRepository userRepository,
                         JobRepository jobRepository,
                         PostRepository postRepository,
                         MessageRepository messageRepository,
                         ModerationService moderationService) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.postRepository = postRepository;
        this.messageRepository = messageRepository;
        this.moderationService = moderationService;
    }

    public ReportDto createReport(String reporterEmail, CreateReportDto request) {
        if (request.getTargetType() == null || request.getTargetId() == null) {
            throw new IllegalArgumentException("Choose what you want to report.");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new IllegalArgumentException("A report reason is required.");
        }

        User reporter = userRepository.findByEmail(reporterEmail)
                .orElseThrow(() -> new RuntimeException("Reporter not found"));
        validateTarget(request.getTargetType(), request.getTargetId(), reporter);

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason().trim());
        report.setDetails(request.getDetails() == null ? "" : request.getDetails().trim());
        report.setRiskScore(calculateRiskScore(request));

        Report savedReport = reportRepository.save(report);

        if (request.getTargetType() == ReportTargetType.USER) {
            userRepository.findById(request.getTargetId()).ifPresent(moderationService::evaluateUser);
        } else if (request.getTargetType() == ReportTargetType.JOB) {
            jobRepository.findById(request.getTargetId()).ifPresent(job -> {
                moderationService.evaluateJob(job);
                jobRepository.save(job);
            });
        }

        return toDto(savedReport);
    }

    public List<ReportDto> getReports(String status) {
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.valueOf(status.toUpperCase()))
                    .stream()
                    .map(this::toDto)
                    .toList();
        }

        return reportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public ReportDto updateStatus(Long id, ReportStatus status) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        report.setStatus(status);
        return toDto(reportRepository.save(report));
    }

    public long pendingCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    private void validateTarget(ReportTargetType targetType, Long targetId, User reporter) {
        switch (targetType) {
            case USER -> {
                User target = userRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("Reported user not found."));
                if (target.getId().equals(reporter.getId())) {
                    throw new IllegalArgumentException("You cannot report your own account.");
                }
            }
            case JOB -> jobRepository.findById(targetId)
                    .orElseThrow(() -> new IllegalArgumentException("Reported job not found."));
            case THREAD -> postRepository.findById(targetId)
                    .orElseThrow(() -> new IllegalArgumentException("Reported thread not found."));
            case MESSAGE_THREAD -> {
                User partner = userRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("Conversation user not found."));
                if (partner.getId().equals(reporter.getId())) {
                    throw new IllegalArgumentException("You cannot report a conversation with yourself.");
                }
                if (messageRepository.findConversation(reporter.getId(), partner.getId()).isEmpty()) {
                    throw new IllegalArgumentException("Conversation not found.");
                }
            }
        }
    }

    private int calculateRiskScore(CreateReportDto request) {
        int score = 1;
        String text = ((request.getReason() == null ? "" : request.getReason()) + " " +
                (request.getDetails() == null ? "" : request.getDetails())).toLowerCase();

        if (text.contains("scam") || text.contains("fraud") || text.contains("arnaque")) score += 5;
        if (text.contains("phishing") || text.contains("link") || text.contains("payment")) score += 4;
        if (text.contains("harassment") || text.contains("toxic") || text.contains("hate")) score += 3;
        if (text.contains("fake") || text.contains("duplicate")) score += 3;
        if (request.getTargetType() == ReportTargetType.JOB) score += 2;
        if (request.getTargetType() == ReportTargetType.MESSAGE_THREAD) score += 2;

        long previousReports = reportRepository.countByTargetTypeAndTargetId(request.getTargetType(), request.getTargetId());
        score += Math.min((int) previousReports, 5);
        return Math.min(score, 15);
    }

    private ReportDto toDto(Report report) {
        ReportDto dto = new ReportDto();
        dto.setId(report.getId());
        dto.setReporterId(report.getReporter().getId());
        dto.setReporterName(report.getReporter().getRealUsername());
        dto.setReporterEmail(report.getReporter().getEmail());
        dto.setTargetType(report.getTargetType());
        dto.setTargetId(report.getTargetId());
        dto.setTargetLabel(resolveTargetLabel(report.getTargetType(), report.getTargetId()));
        dto.setReason(report.getReason());
        dto.setDetails(report.getDetails());
        dto.setStatus(report.getStatus());
        dto.setRiskScore(report.getRiskScore());
        dto.setTargetReportCount(reportRepository.countByTargetTypeAndTargetId(report.getTargetType(), report.getTargetId()));
        dto.setCreatedAt(report.getCreatedAt());
        dto.setUpdatedAt(report.getUpdatedAt());
        return dto;
    }

    private String resolveTargetLabel(ReportTargetType type, Long targetId) {
        return switch (type) {
            case USER -> userRepository.findById(targetId)
                    .map(user -> user.getRealUsername() + " (" + user.getEmail() + ")")
                    .orElse("Deleted user #" + targetId);
            case JOB -> jobRepository.findById(targetId)
                    .map(job -> job.getTitle() + " at " + job.getCompany())
                    .orElse("Deleted job #" + targetId);
            case THREAD -> postRepository.findById(targetId)
                    .map(post -> truncate(post.getContent(), 80))
                    .orElse("Deleted thread #" + targetId);
            case MESSAGE_THREAD -> userRepository.findById(targetId)
                    .map(user -> "Conversation with " + user.getRealUsername())
                    .orElse("Deleted conversation user #" + targetId);
        };
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "Thread";
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }
}
