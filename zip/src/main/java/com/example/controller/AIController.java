package com.example.controller;

import com.example.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
@CrossOrigin
public class AIController {



    @Autowired
    private AIService aiService;


    public AIController(AIService aiService) {
        this.aiService = aiService;

    }


    @GetMapping("/test")
    public String test() {
        String prompt = """
        Say hello in JSON format.

        Return ONLY JSON:
        {
          "message": "hello"
        }
        """;

        return aiService.askAI(prompt, true);
    }

    @PostMapping("/generate-quiz")
    public String generateQuiz(@RequestBody Map<String, String> body) {

        String topic = body.get("topic");

        String prompt = """
You are a JSON API.

Return ONLY valid JSON.
No explanation.

Generate a multiple choice quiz.

Topic: %s

RULES:
- Each question must have 4 options
- The correctAnswer MUST be EXACTLY one of the options (full text, not A/B/C/D)
- Do NOT return letters like A or B

FORMAT:
{
  "questions": [
    {
      "question": "string",
      "options": ["option1","option2","option3","option4"],
      "correctAnswer": "one of the options EXACTLY"
    }
  ]
}
""".formatted(topic);

        return aiService.askAI(prompt, true);
    }

    @PostMapping("/analyze-cv")
    public String analyzeCv(@RequestBody Map<String, String> body) {

        String cvText = body.get("cvText");
        String requirements = body.get("requirements");

        String prompt = """
        You are an expert HR assistant. Your task is to analyze the following CV against the provided job requirements and output a match score as a number between 0 and 100.
        
        Instructions for scoring:
        1. Read the requirements carefully and identify the key skills.
        2. Read the CV carefully and see which of those key skills are present.
        3. Calculate the match score proportionally based ONLY on how many REQUIRED skills are met.
        4. If the CV is completely unrelated to the job requirements, the score MUST be 0.
        5. DO NOT hallucinate skills. If a skill is not explicitly mentioned in the CV, do not count it.
        6. List the exact skills found in the CV that match the requirements.
        
        Job Requirements:
        %s

        Candidate CV:
        %s

        Return exactly and ONLY valid JSON matching this schema:
        {
          "matchScore": 85,
          "skillsFound": ["skill1","skill2"]
        }
        """.formatted(requirements, cvText);

        return aiService.askAI(prompt, true);
    }

}
