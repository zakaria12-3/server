package com.example.dto;

public class QuizSubmissionResultDto {
    private int score;
    private int passingScore;
    private boolean passed;
    private boolean cheatingSuspected;
    private boolean banned;
    private String message;

    public QuizSubmissionResultDto() {
    }

    public QuizSubmissionResultDto(int score, int passingScore, boolean passed, boolean cheatingSuspected, boolean banned, String message) {
        this.score = score;
        this.passingScore = passingScore;
        this.passed = passed;
        this.cheatingSuspected = cheatingSuspected;
        this.banned = banned;
        this.message = message;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(int passingScore) {
        this.passingScore = passingScore;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public boolean isCheatingSuspected() {
        return cheatingSuspected;
    }

    public void setCheatingSuspected(boolean cheatingSuspected) {
        this.cheatingSuspected = cheatingSuspected;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
