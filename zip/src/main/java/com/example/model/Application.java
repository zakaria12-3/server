
package com.example.model;

        import com.example.dto.ApplyResponseDto;
        import jakarta.persistence.*;
        import java.time.LocalDateTime;

@Entity
public class Application extends ApplyResponseDto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Application(Long applicationId, boolean hasQuiz, String status) {
        super(applicationId, hasQuiz, status);
    }

    public Application() {
        super();
    }


    public Boolean getQuizPassed() {
        return quizPassed;
    }

    public void setQuizPassed(Boolean quizPassed) {
        this.quizPassed = quizPassed;
    }

    private Boolean quizPassed;

    public String getCvPath() {
        return cvPath;
    }

    public void setCvPath(String cvPath) {
        this.cvPath = cvPath;
    }

    private String cvPath;

    private LocalDateTime appliedAt;


    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    private String meetingLink;


    @ManyToOne
    @JoinColumn(name = "candidate_id")
    private User candidate;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    public Integer getQuizScore() {
        return quizScore;
    }

    public void setQuizScore(Integer quizScore) {
        this.quizScore = quizScore;
    }

    private Integer quizScore;
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private String status = "PENDING";
    private Boolean cheatingSuspected = false;
    private Integer quizDurationSeconds;
    private Integer quizIntegrityEventCount = 0;

    @Column(length = 1000)
    private String quizIntegrityReason;

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

    @Column(length = 1000)
    private String aiSkillsFound;

    private Integer aiMatchScore;

    public String getAiSkillsFound() {
        return aiSkillsFound;
    }

    public void setAiSkillsFound(String aiSkillsFound) {
        this.aiSkillsFound = aiSkillsFound;
    }

    public Integer getAiMatchScore() {
        return aiMatchScore;
    }

    public void setAiMatchScore(Integer aiMatchScore) {
        this.aiMatchScore = aiMatchScore;
    }



    @ManyToOne
    private Quiz quiz;
    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public User getCandidate() {
        return candidate;
    }

    public void setCandidate(User candidate) {
        this.candidate = candidate;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }


    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public Quiz getQuiz() {
        return quiz;
    }
}
