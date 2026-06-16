package com.example.controller;

import com.example.dto.ApplyResponseDto;
import com.example.dto.ApplicationDto;
import com.example.dto.JobCandidateDto;
import com.example.dto.QuizDto;
import com.example.dto.QuizSubmissionResultDto;
import com.example.service.ApplicationService;
import com.example.service.JobService;
import com.example.service.QuizService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/candidate")
public class CandidateController {




    @GetMapping("/dashboard")
        public String dashboard(){
            return "Candidate dashboard";
        }

             private final JobService jobService;
             private final ApplicationService applicationService;

             private final QuizService quizService;


            public CandidateController(JobService jobService, ApplicationService applicationService,QuizService quizService ) {
            this.jobService = jobService;
            this.applicationService = applicationService;
            this.quizService = quizService;
            }



    @GetMapping("/jobs")
    public List<JobCandidateDto> getJobs(Authentication authentication) {
        String email = authentication.getName();
        return jobService.getJobsForCandidate(email);
    }

    @GetMapping("/recommendations")
    public List<JobCandidateDto> getRecommendations(Authentication authentication) {
        String email = authentication.getName();
        return jobService.getRecommendedJobsForCandidate(email);
    }

    @GetMapping("/applications")
    public List<ApplicationDto> getMyApplications(Authentication authentication) {
        return applicationService.getApplicationsForCandidate(authentication.getName());
    }

    @GetMapping("/applications/{id}")
    public ApplicationDto getMyApplication(@PathVariable Long id, Authentication authentication) {
        return applicationService.getCandidateApplication(id, authentication.getName());
    }


    @PostMapping("/apply")
    public ApplyResponseDto apply(
            @RequestParam("jobId") Long jobId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {



        String email = authentication.getName();
        return applicationService.apply(jobId, email, file);
    }

    @GetMapping("/jobs/{jobId}/quiz")
    public QuizDto getQuiz(@PathVariable Long jobId) {
        return quizService.getQuizDtoByJob(jobId);
    }


    @PostMapping("/jobs/{jobId}/quiz/submit")
    public QuizSubmissionResultDto submitQuiz(
            @PathVariable Long jobId,
            @RequestBody Map<String, Object> answers,
            Authentication authentication
    ) {
        String email = authentication.getName();

        return quizService.submitQuiz(jobId, email, answers);
    }
















    }
