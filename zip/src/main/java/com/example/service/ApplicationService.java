package com.example.service;

import com.example.dto.ApplyResponseDto;
import com.example.dto.ApplicationDto;
import com.example.model.Application;
import com.example.model.Job;
import com.example.model.User;
import com.example.repository.ApplicationRepository;
import com.example.repository.JobRepository;
import com.example.repository.QuizRepository;
import com.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;

@Service
public class ApplicationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationService.class);

    @org.springframework.beans.factory.annotation.Value("${app.upload-dir:uploads/cv}")
    private String uploadDir;

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final EmailService emailService;
    private final AIService aiService;
    private final JobService jobService;

    public ApplicationService(ApplicationRepository applicationRepository,
                              JobRepository jobRepository,
                              UserRepository userRepository,
                              QuizRepository quizRepository,
                              EmailService emailService,
                              AIService aiService,
                              JobService jobService) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
        this.emailService = emailService;
        this.aiService = aiService;
        this.jobService = jobService;
    }

    public ApplyResponseDto apply(Long jobId, String email, MultipartFile file) throws IOException {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!jobService.isAvailable(job)) {
            throw new RuntimeException("This job is no longer accepting applications");
        }

        User candidate = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.findByUsername(email)
                        .orElseThrow(() -> new RuntimeException("User not found")));

        var existingOpt = applicationRepository.findByJobIdAndCandidateId(jobId, candidate.getId());
        if (existingOpt.isPresent()) {
            Application existing = existingOpt.get();
            if (!"PENDING_QUIZ".equals(existing.getStatus())) {
                throw new RuntimeException("You already applied to this job");
            }
        }

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("CV file is required");
        }

        String fileNameOriginal = file.getOriginalFilename();
        if (fileNameOriginal == null || !fileNameOriginal.toLowerCase().endsWith(".pdf")) {
            throw new RuntimeException("Only PDF files are allowed");
        }

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        var quiz = quizRepository.findByJobId(jobId).orElse(null);

        Application app = existingOpt.orElse(new Application());
        app.setJob(job);
        app.setCandidate(candidate);
        app.setCvPath(fileName);
        app.setQuiz(quiz);
        app.setQuizPassed(false);
        app.setQuizScore(0);
        app.setAiMatchScore(null);
        app.setAiSkillsFound(null);
        app.setStatus(quiz != null ? "PENDING_QUIZ" : "PENDING");
        app.setAppliedAt(LocalDateTime.now());
        final Application save = applicationRepository.save(app);

        ApplicationDto analysis;
        try {
            analysis = analyzeApplicationWithAI(save.getId());
        } catch (Exception e) {
            cleanupRejectedApplication(save, filePath);
            throw new RuntimeException("The PDF file does not follow the required standards. Please upload another CV.");
        }

        if (analysis.getAiMatchScore() == null || analysis.getAiMatchScore() <= 0) {
            cleanupRejectedApplication(save, filePath);
            throw new RuntimeException("The PDF file does not follow the required standards. Please upload another CV.");
        }

        notifyRecruiterAboutCvAnalysis(save.getId(), analysis);
        return new ApplyResponseDto(save.getId(), quiz != null, save.getStatus());
    }

    public List<Application> getApplications(Long jobId, String status) {
        if (status != null) {
            return applicationRepository.findByJobIdAndStatus(jobId, status);
        }
        return applicationRepository.findByJobId(jobId).stream()
                .filter(app -> !"PENDING_QUIZ".equals(app.getStatus()))
                .toList();
    }

    public List<ApplicationDto> getApplicationsByJob(Long jobId) {
        List<Application> applications = applicationRepository.findByJobId(jobId);
        return applications.stream()
                .filter(app -> !"PENDING_QUIZ".equals(app.getStatus()))
                .map(this::mapToDto).toList();
    }

    public List<ApplicationDto> getApplicationsForCandidate(String email) {
        User candidate = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.findByUsername(email)
                        .orElseThrow(() -> new RuntimeException("User not found")));

        return applicationRepository.findByCandidateId(candidate.getId())
                .stream()
                .sorted((a, b) -> {
                    LocalDateTime left = a.getAppliedAt() == null ? LocalDateTime.MIN : a.getAppliedAt();
                    LocalDateTime right = b.getAppliedAt() == null ? LocalDateTime.MIN : b.getAppliedAt();
                    return right.compareTo(left);
                })
                .map(this::mapToDto)
                .toList();
    }

    public ApplicationDto getCandidateApplication(Long applicationId, String email) {
        User candidate = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.findByUsername(email)
                        .orElseThrow(() -> new RuntimeException("User not found")));

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (app.getCandidate() == null || !Objects.equals(app.getCandidate().getId(), candidate.getId())) {
            throw new RuntimeException("Not allowed to view this application");
        }

        return mapToDto(app);
    }

    public void assertApplicationBelongsToRecruiter(Long applicationId, String recruiterEmail) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        User recruiter = app.getJob().getRecruiter();
        if (recruiter == null || !Objects.equals(recruiter.getEmail(), recruiterEmail)) {
            throw new RuntimeException("Not allowed to manage this application");
        }
    }

    public List<ApplicationDto> getAllApplications() {
        List<Application> applications = applicationRepository.findAll();
        return applications.stream()
                .filter(app -> !"PENDING_QUIZ".equals(app.getStatus()))
                .map(this::mapToDto).toList();
    }

    private ApplicationDto mapToDto(Application app) {
        ApplicationDto dto = new ApplicationDto();
        dto.setId(app.getId());
        dto.setCandidateName(app.getCandidate().getRealUsername());
        dto.setStatus(app.getStatus());
        dto.setScore(app.getQuizScore());
        dto.setQuizPassed(app.getQuizPassed());
        dto.setCheatingSuspected(app.getCheatingSuspected());
        dto.setQuizDurationSeconds(app.getQuizDurationSeconds());
        dto.setQuizIntegrityEventCount(app.getQuizIntegrityEventCount());
        dto.setQuizIntegrityReason(app.getQuizIntegrityReason());
        if (app.getJob() != null) {
            Job job = app.getJob();
            dto.setJobId(job.getId());
            dto.setJobTitle(job.getTitle());
            dto.setCompany(job.getCompany());
            dto.setCategory(job.getCategory());
            dto.setLocation(job.getLocation());
            dto.setDescription(job.getDescription());
            dto.setExpirationDate(job.getExpirationDate());
            if (job.getRecruiter() != null) {
                dto.setRecruiterName(job.getRecruiter().getRealUsername());
                dto.setRecruiterTitle(job.getRecruiter().getJobTitle());
                dto.setRecruiterEmail(job.getRecruiter().getEmail());
            }
        }
        dto.setAppliedAt(app.getAppliedAt());
        dto.setCvUrl("/files/cv/" + UriUtils.encodePathSegment(app.getCvPath(), StandardCharsets.UTF_8));
        dto.setAiMatchScore(app.getAiMatchScore());
        dto.setAiSkillsFound(app.getAiSkillsFound());
        dto.setMeetingLink(app.getMeetingLink());
        dto.setInterviewDate(app.getInterviewDate());
        return dto;
    }

    public Application updateStatus(Long id, String status, LocalDateTime interviewDate) {
        Application app = applicationRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if ("ACCEPTED".equalsIgnoreCase(status) && interviewDate != null) {
            LocalDateTime startOfDay = interviewDate.toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            long conflicts = applicationRepository.countConflictingInterviews(app.getCandidate().getId(), startOfDay, endOfDay, id);
            if (conflicts > 0) {
                throw new RuntimeException("The candidate already has an interview scheduled on this date.");
            }
            app.setInterviewDate(interviewDate);
        }

        app.setStatus(status);
        String email = app.getCandidate().getEmail();
        String jobTitle = app.getJob().getTitle();

        if ("ACCEPTED".equalsIgnoreCase(status)) {
            if (app.getMeetingLink() == null) {
                String meetingLink = generateMeeting(app.getId());
                app.setMeetingLink(meetingLink);
            }
            emailService.sendInterviewEmail(email, jobTitle, app.getMeetingLink(), app.getInterviewDate());
        } else {
            emailService.sendApplicationResultEmail(email, status, jobTitle);
        }

        return applicationRepository.save(app);
    }

    public String generateMeeting(Long applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        String roomName = "job-" + app.getJob().getId() + "-user-" + app.getCandidate().getId();
        String link = "https://meet.jit.si/" + roomName;
        app.setMeetingLink(link);
        applicationRepository.save(app);
        return link;
    }

    public Application updateMeetingLink(Long applicationId, String meetingLink) {
        if (meetingLink == null || meetingLink.isBlank()) {
            throw new RuntimeException("Meeting link is required");
        }

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        app.setMeetingLink(meetingLink.trim());
        return applicationRepository.save(app);
    }

    public Path getCvPathForRecruiter(Long applicationId, String recruiterEmail) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        User recruiter = app.getJob().getRecruiter();
        if (recruiter == null || !Objects.equals(recruiter.getEmail(), recruiterEmail)) {
            throw new RuntimeException("Not allowed to view this CV");
        }

        if (app.getCvPath() == null || app.getCvPath().isBlank()) {
            throw new RuntimeException("No CV found for this application");
        }

        Path uploadDirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path cvPath = uploadDirPath.resolve(app.getCvPath()).normalize();
        if (!cvPath.startsWith(uploadDirPath)) {
            throw new RuntimeException("Invalid CV path");
        }

        return cvPath;
    }

    public void deleteApplication(Long id) {
        applicationRepository.deleteById(id);
    }

    public List<ApplicationDto> getRankedCandidates(Long jobId) {
        return applicationRepository.findByJobId(jobId)
                .stream()
                .filter(app -> !"PENDING_QUIZ".equals(app.getStatus()))
                .filter(app -> app.getAiMatchScore() != null)
                .sorted((a, b) -> Integer.compare(b.getAiMatchScore(), a.getAiMatchScore()))
                .map(this::mapToDto)
                .toList();
    }

    private void analyzeUploadedCvAndNotifyRecruiter(Long applicationId) {
        try {
            ApplicationDto dto = analyzeApplicationWithAI(applicationId);
            notifyRecruiterAboutCvAnalysis(applicationId, dto);
        } catch (Exception e) {
            LOGGER.error("Automatic CV analysis failed for application {}", applicationId, e);
        }
    }

    private void notifyRecruiterAboutCvAnalysis(Long applicationId, ApplicationDto dto) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        User recruiter = app.getJob().getRecruiter();
        if (recruiter != null && recruiter.getEmail() != null && !recruiter.getEmail().isBlank()) {
            emailService.sendCvAnalysisEmail(
                    recruiter.getEmail(),
                    dto.getCandidateName(),
                    dto.getJobTitle(),
                    dto.getAiMatchScore(),
                    dto.getAiSkillsFound()
            );
        }
    }

    private void cleanupRejectedApplication(Application app, Path filePath) {
        try {
            applicationRepository.deleteById(app.getId());
        } catch (Exception e) {
            LOGGER.warn("Could not delete rejected application {}", app.getId(), e);
        }
        try {
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            LOGGER.warn("Could not delete rejected CV file {}", filePath, e);
        }
    }

    public ApplicationDto analyzeApplicationWithAI(Long applicationId) throws Exception {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (app.getCvPath() == null) {
            throw new RuntimeException("No CV found for this application");
        }

        byte[] pdfBytes;
        Path cvPath = Paths.get(uploadDir).resolve(app.getCvPath()).toAbsolutePath().normalize();
        pdfBytes = Files.readAllBytes(cvPath);

        String cvText = "";
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            cvText = stripper.getText(document);
            if (cvText.length() > 8000) {
                    cvText = cvText.substring(0, 8000);
                                        }
        }

        String jobTitle = app.getJob().getTitle();
        String requirements = app.getJob().getDescription();
        String prompt = """
        You are a strict and highly accurate HR assistant evaluating candidate CVs.
        Your task is to analyze the provided Candidate Text against the Job Title and Job Requirements, and return a match score (0-100) and a list of found skills.

        STRICT RULES:
        1. DOCUMENT VALIDATION: First, verify if the "Candidate Text" is actually a CV/Resume. If it looks like a receipt, an invoice, a medical document, a random text, or anything other than a CV, you MUST return a matchScore of 0 and an empty list for skillsFound.
        2. REQUIREMENT MATCHING: Identify the key skills and qualifications in the Job Title and Job Requirements.
        3. STRICT SKILL EXTRACTION: Only extract skills that are EXPLICITLY written in the Candidate Text. DO NOT hallucinate, infer, or assume skills.
        4. SCORING: Calculate the score based ONLY on the exact match of required skills found in the Candidate Text. If no required skills are found, the score MUST be 0.

        Job Title: %s

        Job Requirements:
        %s

        Candidate Text:
        %s

        Return exactly and ONLY valid JSON matching this schema, with no additional text or markdown formatting:
        {
          "matchScore": <number between 0 and 100>,
          "skillsFound": ["<skill1>", "<skill2>"]
        }
        """.formatted(jobTitle, requirements, cvText);

        String aiResponse = aiService.askAI(prompt, true);

        int start = aiResponse.indexOf("{");
        int end = aiResponse.lastIndexOf("}") + 1;

        if (start == -1 || end == 0 || start >= end) {
            throw new RuntimeException("AI did not return valid JSON. Raw: " + aiResponse);
        }

        String jsonOnly = aiResponse.substring(start, end);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> parsed = mapper.readValue(jsonOnly, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){});

        Integer matchScore = null;
        if (parsed.get("matchScore") instanceof Number num) {
            matchScore = num.intValue();
        }

        String skillsStr = "";
        if (parsed.get("skillsFound") instanceof List<?> list) {
            skillsStr = String.join(", ", list.stream().map(Object::toString).toList());
        }

        app.setAiMatchScore(matchScore);
        app.setAiSkillsFound(skillsStr);
        applicationRepository.save(app);

        return mapToDto(app);
    }
}
