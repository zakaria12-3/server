package com.example.controller;


import com.example.model.User;
import com.example.model.Job;
import com.example.dto.ApplicationDto;
import com.example.service.UserService;
import com.example.service.JobService;
import com.example.service.ApplicationService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;

    public AdminController(UserService userService, JobService jobService, ApplicationService applicationService) {
        this.userService = userService;
        this.jobService = jobService;
        this.applicationService = applicationService;
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

    @org.springframework.web.bind.annotation.DeleteMapping("/jobs/{id}")
    public org.springframework.http.ResponseEntity<?> deleteJob(@org.springframework.web.bind.annotation.PathVariable Long id) {
        jobService.deleteJob(id);
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/applications/{id}")
    public org.springframework.http.ResponseEntity<?> deleteApplication(@org.springframework.web.bind.annotation.PathVariable Long id) {
        applicationService.deleteApplication(id);
        return org.springframework.http.ResponseEntity.ok().build();
    }

}
