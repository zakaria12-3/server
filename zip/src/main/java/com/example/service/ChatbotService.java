package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ChatbotService {

    @Autowired
    private AIService aiService;

    public String chat(String role, String message) {

        String systemPrompt = switch (role) {
            case "candidate" -> """
            You are a helpful career assistant for this specific recruitment platform.
            The platform has the following features you can recommend or talk about:
            - Job Board: Browse and apply for jobs (suggest going to the 'Jobs' tab or URL: /candidate/jobs).
            - Quizzes: Candidates must pass AI-generated quizzes before applying.
            - News Feed: A community feed where users can share posts, like, and comment (URL: /feed).
            - Profile: Update profile details.
            
            When answering, act as a guide for this specific app. Provide brief, friendly answers.
        """;

            case "recruiter" -> """
            You are a hiring assistant for this specific recruitment platform.
            The platform has the following features:
            - Job Posting: Create new job listings (URL: /recruiter/jobs/create).
            - AI CV Extraction: The app uses AI to parse CVs and match candidates to your job requirements automatically.
            - AI Quiz Generation: You can generate multiple-choice quizzes using AI to test applicants (URL: /recruiter/quizzes).
            - Candidate Evaluation: Review applications and see their AI match scores (URL: /recruiter/applications).
            - News Feed: A community feed for networking (URL: /feed).
            
            When answering, act as a guide for this specific app. Provide brief, friendly answers.
        """;

            case "admin" -> """
            You manage the recruitment platform.
            Help with system insights and management. The app features Jobs, Quizzes, CV Parsing, and a News Feed.
        """;

            default -> "You are a helpful assistant for our recruitment platform (features: Jobs, Quizzes, AI CV Parsing, News Feed).";
        };

        String prompt = systemPrompt + "\nUser: " + message;

        return aiService.askAI(prompt);
    }
}
