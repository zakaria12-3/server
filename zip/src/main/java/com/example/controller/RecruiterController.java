
package com.example.controller;


import com.example.dto.ApplicationDto;
import com.example.dto.CreateQuizDto;
import com.example.dto.JobDto;
import com.example.model.Application;
import com.example.model.Job;
import com.example.model.User;
import com.example.repository.UserRepository;
import com.example.service.ApplicationService;
import com.example.service.JobService;
import com.example.service.QuizService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recruiter")
public class RecruiterController {
    @GetMapping( "/dashboard")
    public String dashboard() {
        return "Recruiter dashboard";
    }

    private final JobService jobService;

    private final QuizService quizService;
    private final ApplicationService applicationService;

    private final UserRepository userRepository;

    public RecruiterController(JobService jobService,ApplicationService applicationService,UserRepository userRepository, QuizService quizService) {
        this.jobService = jobService;
        this.quizService = quizService;
        this.applicationService=applicationService;
        this.userRepository = userRepository;

    }
    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id, Authentication authentication) {
        jobService.deleteRecruiterJob(id, currentRecruiter(authentication));
        return ResponseEntity.ok().build();
    }
    @GetMapping("/jobs")
    public List<Job> getRecruiterJobs(Authentication authentication) {
        User recruiter = currentRecruiter(authentication);
        jobService.ensureRecruiterCanManageJobs(recruiter);
        return jobService.getJobsByRecruiter(recruiter.getId());
    }
    @PostMapping("/jobs")
    public Job createJob(@RequestBody JobDto dto, Authentication authentication) {

        User recruiter = currentRecruiter(authentication);

        Job job = new Job();
        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setLocation(dto.getLocation());
        job.setCompany(recruiter.getCompany() != null ? recruiter.getCompany().getName() : null);
        job.setExpirationDate(dto.getExpirationDate());

        return jobService.createJob(job, recruiter);
    }

    @GetMapping("/jobs/{id}")
    public Job getJob(@PathVariable Long id, Authentication authentication) {
        User recruiter = currentRecruiter(authentication);

        return jobService.getRecruiterJob(id, recruiter);
    }

    @PutMapping("/jobs/{id}")
    public Job updateJob(@PathVariable Long id, @RequestBody JobDto dto, Authentication authentication) {
        User recruiter = currentRecruiter(authentication);

        Job job = new Job();
        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setLocation(dto.getLocation());
        job.setCompany(recruiter.getCompany() != null ? recruiter.getCompany().getName() : null);
        job.setExpirationDate(dto.getExpirationDate());

        return jobService.updateJob(id, job, recruiter);
    }



    @PostMapping("/applications/{id}/meet")
    public String createMeeting(@PathVariable Long id, Authentication authentication) {
        applicationService.assertApplicationBelongsToRecruiter(id, authentication.getName());
        return applicationService.generateMeeting(id);
    }

    @PutMapping("/applications/{id}/meeting")
    public Application updateMeetingLink(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication authentication) {
        applicationService.assertApplicationBelongsToRecruiter(id, authentication.getName());
        return applicationService.updateMeetingLink(id, body.get("meetingLink"));
    }

    @GetMapping("/applications/{id}/cv")
    public ResponseEntity<Resource> getApplicationCv(@PathVariable Long id, Authentication authentication) throws Exception {
        Path path = applicationService.getCvPathForRecruiter(id, authentication.getName());
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        String filename = path.getFileName().toString();
        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_PDF_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(resource);
    }

    @GetMapping("/cv/{filename}")
    public ResponseEntity<Resource> getCv(@PathVariable String filename) throws Exception {

        Path path = Paths.get("uploads/cv").resolve(filename).toAbsolutePath().normalize();
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    @GetMapping("/jobs/{jobId}/applications")
    public List<ApplicationDto> getApplications(@PathVariable("jobId") Long jobId, Authentication authentication) {
        jobService.getRecruiterJob(jobId, currentRecruiter(authentication));
        return applicationService.getApplicationsByJob(jobId);
    }
    @PutMapping("/applications/{id}/status")
    public Application updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            Authentication authentication) {
        applicationService.assertApplicationBelongsToRecruiter(id, authentication.getName());
        return applicationService.updateStatus(id, status);
    }
    @PostMapping("/quiz")
    public CreateQuizDto createQuiz(@RequestBody CreateQuizDto dto, Authentication authentication) {
        User recruiter = currentRecruiter(authentication);
        jobService.getRecruiterJob(dto.getJobId(), recruiter);
        quizService.createQuiz(dto);
        return quizService.getEditableQuizDtoByJob(dto.getJobId());
    }

    @GetMapping("/jobs/{jobId}/quiz")
    public CreateQuizDto getQuiz(@PathVariable Long jobId, Authentication authentication) {
        User recruiter = currentRecruiter(authentication);
        jobService.getRecruiterJob(jobId, recruiter);
        return quizService.getEditableQuizDtoByJob(jobId);
    }

    @PutMapping("/jobs/{jobId}/quiz")
    public CreateQuizDto updateQuiz(@PathVariable Long jobId, @RequestBody CreateQuizDto dto, Authentication authentication) {
        User recruiter = currentRecruiter(authentication);
        jobService.getRecruiterJob(jobId, recruiter);
        quizService.updateQuiz(jobId, dto);
        return quizService.getEditableQuizDtoByJob(jobId);
    }

    @DeleteMapping("/jobs/{jobId}/quiz")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long jobId, Authentication authentication) {
        User recruiter = currentRecruiter(authentication);
        jobService.getRecruiterJob(jobId, recruiter);
        quizService.deleteQuizForJob(jobId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/generate-quiz")
    public ResponseEntity<?> generateQuiz(@RequestBody Map<String, Object> body, Authentication authentication) {
        try {
            String topic = (String) body.get("topic");
            Long jobId = Long.valueOf(body.get("jobId").toString());
            User recruiter = currentRecruiter(authentication);
            jobService.getRecruiterJob(jobId, recruiter);
            return ResponseEntity.ok(quizService.generateQuizDraft(topic, jobId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "hint", "If the error mentions 'no JSON found', the AI model returned plain text. Try again or switch to a larger model."
            ));
        }
    }
    @GetMapping("/jobs/{jobId}/ranking")
    public List<ApplicationDto> getRanking(@PathVariable Long jobId, Authentication authentication) {
        jobService.getRecruiterJob(jobId, currentRecruiter(authentication));
        return applicationService.getRankedCandidates(jobId);
    }



    @PostMapping("/applications/{id}/analyze")
    public ApplicationDto analyzeApplication(@PathVariable Long id, Authentication authentication) throws Exception {
        applicationService.assertApplicationBelongsToRecruiter(id, authentication.getName());
        return applicationService.analyzeApplicationWithAI(id);
    }

    private User currentRecruiter(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }


}
