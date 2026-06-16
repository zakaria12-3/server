package com.example.dto;

public class ApplyResponseDto {
    private Long applicationId;
    private boolean hasQuiz;
    private String status;

    public ApplyResponseDto(Long applicationId, boolean hasQuiz, String status) {
        this.applicationId = applicationId;
        this.hasQuiz = hasQuiz;
        this.status = status;
    }

    public ApplyResponseDto() {

    }

    public Long getApplicationId() { return applicationId; }
    public boolean isHasQuiz() { return hasQuiz; }
    public String getStatus() { return status; }
}
