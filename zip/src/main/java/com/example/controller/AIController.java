package com.example.controller;

import com.example.service.AIService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@CrossOrigin
public class AIController {

    private final AIService aiService;

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
        String jobTitle = body.get("jobTitle");       // ← was missing
        String requirements = body.get("requirements");
        String prompt = """
You are a strict HR assistant evaluating candidate CVs.

STEP 1 - DOCUMENT VALIDATION (do this first):
Carefully examine the Candidate Text. A valid CV must contain AT LEAST 3 of these elements:
- A person's full name
- Contact information (email, phone, address)
- Work experience or internships
- Education (university, degree, graduation year)
- A list of technical or professional skills

If the document does NOT contain at least 3 of these elements, it is NOT a CV.
This includes: diagrams, use case documents, UML diagrams, flowcharts, reports,
invoices, receipts, academic papers, random text, or any non-CV document.
In that case, return IMMEDIATELY: {"matchScore": 0, "skillsFound": []}

STEP 2 - SCORING (only if document passed Step 1):
- Extract ONLY skills EXPLICITLY written in the CV text
- Score based ONLY on exact matches with job requirements
- Do NOT infer, assume, or hallucinate skills

Job Title: %s

Job Requirements:
%s

Candidate Text:
%s

Return ONLY valid JSON, no markdown, no extra text:
{
  "matchScore": <number 0-100>,
  "skillsFound": ["<skill1>", "<skill2>"]
}
""".formatted(jobTitle, requirements, cvText);
        return aiService.askAI(prompt, true);
    }
}
