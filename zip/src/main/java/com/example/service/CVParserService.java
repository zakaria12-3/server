package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CVParserService {
    @Autowired
    private AIService aiService;

    public String extractData(String cvText) {

        String prompt = """
    Extract structured data from this CV:
    - Skills
    - Experience (years)
    - Job titles

    Return JSON only.

    CV:
    """ + cvText;

        return aiService.askAI(prompt);
    }


}
