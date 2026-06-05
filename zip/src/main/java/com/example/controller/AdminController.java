package com.example.controller;


import com.example.model.User;
import com.example.model.Job;
import com.example.model.ActionLog;
import com.example.dto.ApplicationDto;
import com.example.service.UserService;
import com.example.service.JobService;
import com.example.service.ApplicationService;
import com.example.service.ActionLogService;
import com.example.service.ReportService;
import com.example.service.QuizService;
import com.example.dto.ReportDto;
import com.example.model.ReportStatus;
import com.example.model.Quiz;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final ActionLogService actionLogService;
    private final ReportService reportService;
    private final QuizService quizService;

    public AdminController(UserService userService, JobService jobService, ApplicationService applicationService, ActionLogService actionLogService, ReportService reportService, QuizService quizService) {
        this.userService = userService;
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.actionLogService = actionLogService;
        this.reportService = reportService;
        this.quizService = quizService;
    }

    @GetMapping("/dashboard")
    public String dashboard(){
        return "Admin dashboard";
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userService.allUsers();
    }

    @GetMapping("/jobs")
    public List<Job> getAllJobs() {
        return jobService.getAllJobs();
    }

    @GetMapping("/applications")
    public List<ApplicationDto> getAllApplications() {
        return applicationService.getAllApplications();
    }

    @GetMapping("/action-logs")
    public List<ActionLog> getActionLogs(@org.springframework.web.bind.annotation.RequestParam(required = false) String role) {
        return actionLogService.getRecentLogs(role);
    }

    @GetMapping("/quizzes")
    public List<Quiz> getAllQuizzes() {
        return quizService.getAllQuizzes();
    }

    @GetMapping("/reports")
    public List<ReportDto> getReports(@org.springframework.web.bind.annotation.RequestParam(required = false) String status) {
        return reportService.getReports(status);
    }

    @org.springframework.web.bind.annotation.PutMapping("/reports/{id}/status")
    public org.springframework.http.ResponseEntity<?> updateReportStatus(@org.springframework.web.bind.annotation.PathVariable Long id, @org.springframework.web.bind.annotation.RequestParam ReportStatus status) {
        return org.springframework.http.ResponseEntity.ok(reportService.updateStatus(id, status));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/users/{id}")
    public org.springframework.http.ResponseEntity<?> deleteUser(@org.springframework.web.bind.annotation.PathVariable Long id) {
        userService.deleteUser(id);
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @org.springframework.web.bind.annotation.PutMapping("/users/{id}/role")
    public org.springframework.http.ResponseEntity<?> updateUserRole(@org.springframework.web.bind.annotation.PathVariable Long id, @org.springframework.web.bind.annotation.RequestParam com.example.model.Role role) {
        return org.springframework.http.ResponseEntity.ok(userService.updateUserRole(id, role));
    }

    @org.springframework.web.bind.annotation.PutMapping("/users/{id}/status")
    public org.springframework.http.ResponseEntity<?> updateUserStatus(@org.springframework.web.bind.annotation.PathVariable Long id, @org.springframework.web.bind.annotation.RequestParam boolean enabled) {
        return org.springframework.http.ResponseEntity.ok(userService.updateUserStatus(id, enabled));
    }

    @org.springframework.web.bind.annotation.PutMapping("/users/{id}/approve")
    public org.springframework.http.ResponseEntity<?> approveRecruiter(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return org.springframework.http.ResponseEntity.ok(userService.approveRecruiter(id));
    }

    @org.springframework.web.bind.annotation.PutMapping("/users/{id}/reject")
    public org.springframework.http.ResponseEntity<?> rejectRecruiter(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return org.springframework.http.ResponseEntity.ok(userService.rejectRecruiter(id));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/jobs/{id}")
    public org.springframework.http.ResponseEntity<?> deleteJob(@org.springframework.web.bind.annotation.PathVariable Long id) {
        jobService.deleteJob(id);
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @org.springframework.web.bind.annotation.PutMapping("/jobs/{id}/approve")
    public Job approveJob(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return jobService.approveJob(id);
    }

    @org.springframework.web.bind.annotation.PutMapping("/jobs/{id}/block")
    public Job blockJob(@org.springframework.web.bind.annotation.PathVariable Long id, @org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return jobService.blockJob(id, reason);
    }

    @org.springframework.web.bind.annotation.PutMapping("/jobs/{id}/rescan")
    public Job rescanJob(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return jobService.rescanJob(id);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/applications/{id}")
    public org.springframework.http.ResponseEntity<?> deleteApplication(@org.springframework.web.bind.annotation.PathVariable Long id) {
        applicationService.deleteApplication(id);
        return org.springframework.http.ResponseEntity.ok().build();
    }

}
