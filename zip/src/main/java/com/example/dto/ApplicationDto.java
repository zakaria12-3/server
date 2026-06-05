package com.example.dto;

import java.time.LocalDateTime;

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
    private LocalDateTime appliedAt;
    private String meetingLink;

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

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
