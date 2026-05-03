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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.io.File;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ApplicationService {


    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final EmailService emailService;
    private final AIService aiService;

    public ApplicationService(ApplicationRepository applicationRepository,
                              JobRepository jobRepository,
                              UserRepository userRepository,
                              QuizRepository quizRepository,
                              EmailService emailService,
                              AIService aiService) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
        this.emailService = emailService;
        this.aiService = aiService;
    }



    public ApplyResponseDto apply(Long jobId, String email, MultipartFile file) throws IOException {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        User candidate = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.findByUsername(email)
                        .orElseThrow(() -> new RuntimeException("User not found")));

        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidate.getId())) {
            throw new RuntimeException("You already applied to this job");
        }

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("CV file is required");
        }

        String fileNameOriginal = file.getOriginalFilename();
        if (fileNameOriginal == null || !fileNameOriginal.toLowerCase().endsWith(".pdf")) {
            throw new RuntimeException("Only PDF files are allowed");
        }

        String uploadDir = System.getProperty("user.dir") + "/uploads/cv/";
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        var quiz = quizRepository.findByJobId(jobId).orElse(null);

        Application app = new Application();
        app.setJob(job);
        app.setCandidate(candidate);
        app.setCvPath(fileName);
        app.setQuiz(quiz);
        app.setQuizPassed(false);
        app.setQuizScore(0);
        app.setStatus("PENDING");
        app.setAppliedAt(LocalDateTime.now());
        final Application save = applicationRepository.save(app);
        return new ApplyResponseDto(save.getId(), quiz != null, save.getStatus());
    }

    public List<Application> getApplications(Long jobId, String status) {

        if (status != null) {
            return applicationRepository.findByJobIdAndStatus(jobId, status);
        }

        return applicationRepository.findByJobId(jobId);
    }

    public List<ApplicationDto> getApplicationsByJob(Long jobId) {
        List<Application> applications = applicationRepository.findByJobId(jobId);
        return applications.stream().map(this::mapToDto).toList();
    }

    public List<ApplicationDto> getAllApplications() {
        List<Application> applications = applicationRepository.findAll();
        return applications.stream().map(this::mapToDto).toList();
    }

    private ApplicationDto mapToDto(Application app) {
        ApplicationDto dto = new ApplicationDto();
        dto.setId(app.getId());
        dto.setCandidateName(app.getCandidate().getRealUsername());
        dto.setStatus(app.getStatus());
        dto.setScore(app.getQuizScore());
        dto.setQuizPassed(app.getQuizPassed());
        dto.setJobTitle(app.getJob().getTitle());
        dto.setCvUrl("http://localhost:8027/files/cv/" + app.getCvPath());
        dto.setAiMatchScore(app.getAiMatchScore());
        dto.setAiSkillsFound(app.getAiSkillsFound());
        return dto;
    }

    public Application updateStatus(Long id, String status) {

        Application app = applicationRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Application not found"));


        //ACCEPTED   ~ REJECTED ~ PENDING
        app.setStatus(status);
        String email = app.getCandidate().getEmail();
        String jobTitle = app.getJob().getTitle();

        if ("ACCEPTED".equalsIgnoreCase(status)) {
            if (app.getMeetingLink() == null) {
                String meetingLink = generateMeeting(app.getId());
                app.setMeetingLink(meetingLink);
                emailService.sendInterviewEmail(
                        email,
                        jobTitle,
                        meetingLink
                );
            }} else {
                emailService.sendApplicationResultEmail(
                        email,
                        status, jobTitle
                );
            }


            return applicationRepository.save(app);


        }
        public String generateMeeting(Long applicationId){
            Application app = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new RuntimeException("Application not found"));
            String roomName = "job-" + app.getJob().getId() + "-user-" + app.getCandidate().getId();
            String link = "https://meet.jit.si/" + roomName;
            applicationRepository.save(app);

            return link;
        }

    public void deleteApplication(Long id) {
        applicationRepository.deleteById(id);
    }

    public List<ApplicationDto> getRankedCandidates(Long jobId) {
        return applicationRepository.findByJobId(jobId)
                .stream()
                .filter(app -> app.getAiMatchScore() != null)
                .sorted((a, b) -> Integer.compare(b.getAiMatchScore(), a.getAiMatchScore())) // highest first
                .map(this::mapToDto)
                .toList();
    }

    public ApplicationDto analyzeApplicationWithAI(Long applicationId) throws Exception {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (app.getCvPath() == null) {
            throw new RuntimeException("No CV found for this application");
        }

        File cvFile = new File(System.getProperty("user.dir") + "/uploads/cv/" + app.getCvPath());
        if (!cvFile.exists()) {
            throw new RuntimeException("CV file not found on disk");
        }

        String cvText = "";
        try (PDDocument document = PDDocument.load(cvFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            cvText = stripper.getText(document);
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
        4. SCORING: Calculate the score based ONLY on the exact match of required skills found in the Candidate Text. If no required skills are found, the score MUST be 0. If it's completely unrelated, the score MUST be 0.
        
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
        Map<String, Object> parsed = mapper.readValue(jsonOnly, Map.class);

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