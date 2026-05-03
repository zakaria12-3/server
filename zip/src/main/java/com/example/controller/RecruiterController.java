
package com.example.controller;


import com.example.dto.ApplicationDto;
import com.example.dto.CreateQuizDto;
import com.example.dto.JobDto;
import com.example.model.Application;
import com.example.model.Job;
import com.example.model.Quiz;
import com.example.model.User;
import com.example.repository.UserRepository;
import com.example.service.ApplicationService;
import com.example.service.JobService;
import com.example.service.QuizService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/jobs")
    public List<Job> getRecruiterJobs(Authentication authentication) {
        String email = authentication.getName();

        User recruiter = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return jobService.getJobsByRecruiter(recruiter.getId());
    }
    @PostMapping("/jobs")
    public Job createJob(@RequestBody JobDto dto, Authentication authentication) {

        String email = authentication.getName();

        User recruiter = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Job job = new Job();
        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setLocation(dto.getLocation());
        job.setCompany(dto.getCompany());

        return jobService.createJob(job, recruiter);
    }



    @PostMapping("/applications/{id}/meet")
    public String createMeeting(@PathVariable Long id) {
        return applicationService.generateMeeting(id);
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
    public List<ApplicationDto> getApplications(@PathVariable("jobId") Long jobId) {
        return applicationService.getApplicationsByJob(jobId);
    }
    @PutMapping("/applications/{id}/status")
    public Application updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status) {
        return applicationService.updateStatus(id, status);
    }
    @PostMapping("/quiz")
    public Quiz createQuiz(@RequestBody CreateQuizDto dto) {
        return quizService.createQuiz(dto);
    }

    @GetMapping("/jobs/{jobId}/quiz")
    public Quiz getQuiz(@PathVariable Long jobId) {
        return quizService.getQuizEntityByJob(jobId);
    }

    @PutMapping("/jobs/{jobId}/quiz")
    public Quiz updateQuiz(@PathVariable Long jobId, @RequestBody CreateQuizDto dto) {
        return quizService.updateQuiz(jobId, dto);
    }
    @PostMapping("/generate-quiz")
    public ResponseEntity<?> generateQuiz(@RequestBody Map<String, Object> body) {
        try {
            String topic = (String) body.get("topic");
            Long jobId = Long.valueOf(body.get("jobId").toString());
            Quiz quiz = quizService.generateAndSaveQuiz(topic, jobId);
            return ResponseEntity.ok(quiz);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "hint", "If the error mentions 'no JSON found', the AI model returned plain text. Try again or switch to a larger model."
            ));
        }
    }
    @GetMapping("/jobs/{jobId}/ranking")
    public List<ApplicationDto> getRanking(@PathVariable Long jobId) {
        return applicationService.getRankedCandidates(jobId);
    }



    @PostMapping("/applications/{id}/analyze")
    public ApplicationDto analyzeApplication(@PathVariable Long id) throws Exception {
        return applicationService.analyzeApplicationWithAI(id);
    }


}
