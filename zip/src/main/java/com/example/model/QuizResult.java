package com.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class QuizResult {
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
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

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getIntegrityEventCount() {
        return integrityEventCount;
    }

    public void setIntegrityEventCount(Integer integrityEventCount) {
        this.integrityEventCount = integrityEventCount;
    }

    public Boolean getCheatingSuspected() {
        return cheatingSuspected;
    }

    public void setCheatingSuspected(Boolean cheatingSuspected) {
        this.cheatingSuspected = cheatingSuspected;
    }

    public String getIntegrityReason() {
        return integrityReason;
    }

    public void setIntegrityReason(String integrityReason) {
        this.integrityReason = integrityReason;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int score;

    private Integer durationSeconds;
    private Integer integrityEventCount = 0;
    private Boolean cheatingSuspected = false;

    @Column(length = 1000)
    private String integrityReason;

    private LocalDateTime submittedAt;

    @ManyToOne
    private User candidate;

    @ManyToOne
    private Job job;
    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;
}
