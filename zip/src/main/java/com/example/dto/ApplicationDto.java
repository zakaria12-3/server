package com.example.dto;

import java.time.LocalDateTime;
import java.time.LocalDate;

public class ApplicationDto {

    private Long id;

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    private Integer score;
    private String candidateName;
    private String cvUrl;
    private String status;
    private Boolean quizPassed;
    private Boolean cheatingSuspected;
    private Integer quizDurationSeconds;
    private Integer quizIntegrityEventCount;
    private String quizIntegrityReason;
    private String jobTitle;
    private Long jobId;
    private String company;
    private String category;
    private String location;
    private String description;
    private LocalDate expirationDate;
    private String recruiterName;
    private String recruiterTitle;
    private String recruiterEmail;
    private LocalDateTime appliedAt;
    private String meetingLink;
    private LocalDateTime interviewDate;

    public LocalDateTime getInterviewDate() { return interviewDate; }
    public void setInterviewDate(LocalDateTime interviewDate) { this.interviewDate = interviewDate; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public String getRecruiterName() { return recruiterName; }
    public void setRecruiterName(String recruiterName) { this.recruiterName = recruiterName; }

    public String getRecruiterTitle() { return recruiterTitle; }
    public void setRecruiterTitle(String recruiterTitle) { this.recruiterTitle = recruiterTitle; }

    public String getRecruiterEmail() { return recruiterEmail; }
    public void setRecruiterEmail(String recruiterEmail) { this.recruiterEmail = recruiterEmail; }

    public Boolean getQuizPassed() {
        return quizPassed;
    }

    public void setQuizPassed(Boolean quizPassed) {
        this.quizPassed = quizPassed;
    }

    public Boolean getCheatingSuspected() {
        return cheatingSuspected;
    }

    public void setCheatingSuspected(Boolean cheatingSuspected) {
        this.cheatingSuspected = cheatingSuspected;
    }

    public Integer getQuizDurationSeconds() {
        return quizDurationSeconds;
    }

    public void setQuizDurationSeconds(Integer quizDurationSeconds) {
        this.quizDurationSeconds = quizDurationSeconds;
    }

    public Integer getQuizIntegrityEventCount() {
        return quizIntegrityEventCount;
    }

    public void setQuizIntegrityEventCount(Integer quizIntegrityEventCount) {
        this.quizIntegrityEventCount = quizIntegrityEventCount;
    }

    public String getQuizIntegrityReason() {
        return quizIntegrityReason;
    }

    public void setQuizIntegrityReason(String quizIntegrityReason) {
        this.quizIntegrityReason = quizIntegrityReason;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getCvUrl() {
        return cvUrl;
    }

    public void setCvUrl(String cvUrl) {
        this.cvUrl = cvUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    private Integer aiMatchScore;
    private String aiSkillsFound;

    public Integer getAiMatchScore() {
        return aiMatchScore;
    }

    public void setAiMatchScore(Integer aiMatchScore) {
        this.aiMatchScore = aiMatchScore;
    }

    public String getAiSkillsFound() {
        return aiSkillsFound;
    }

    public void setAiSkillsFound(String aiSkillsFound) {
        this.aiSkillsFound = aiSkillsFound;
    }
}
