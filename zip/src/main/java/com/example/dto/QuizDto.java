package com.example.dto;

import java.util.List;

public class QuizDto {

    private Long id;




    private String jobTitle;
    private List<QuestionDto> questions;
    private Integer passingScore;

    public QuizDto(Long id, List<QuestionDto> questions, Integer passingScore, String jobTitle) {
        this.id = id;
        this.jobTitle= jobTitle;
        this.questions = questions;
        this.passingScore = passingScore;
    }
    public QuizDto(){}

    public Long getId() { return id; }
    public List<QuestionDto> getQuestions() { return questions; }
    public Integer getPassingScore() { return passingScore; }
    public String getJobTitle() {
        return jobTitle;
    }
}