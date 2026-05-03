package com.example.dto;

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
    private String jobTitle;

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public Boolean getQuizPassed() {
        return quizPassed;
    }

    public void setQuizPassed(Boolean quizPassed) {
        this.quizPassed = quizPassed;
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