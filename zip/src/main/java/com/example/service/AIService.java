package com.example.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.net.*;
import java.util.*;

@Service
public class AIService {
    @Value("${GROQ_API_KEY}")
    private String apiKey;
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    public String askAI(String prompt) {
        return askAI(prompt, false);
    }

    public String askAI(String prompt, boolean requireJson) {
        try {
            URL url = new URL(GROQ_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);

            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> body = new HashMap<>();
            body.put("model", "llama-3.1-8b-instant"); // ← updated model
            body.put("messages", List.of(message));
            body.put("max_tokens", 1024);
            if (requireJson) {
                body.put("response_format", Map.of("type", "json_object"));
            }

            try (OutputStream os = conn.getOutputStream()) {
                os.write(mapper.writeValueAsBytes(body));
            }

            int statusCode = conn.getResponseCode();
            BufferedReader br = new BufferedReader(
                new InputStreamReader(
                    statusCode < 300
                        ? conn.getInputStream()
                        : conn.getErrorStream()
                )
            );

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) response.append(line);
            if (statusCode >= 300) {
                System.err.println("Groq API error " + statusCode + ": " + response);
                return "AI error: " + response;
            }

            Map<String, Object> json = mapper.readValue(response.toString(), Map.class);
            List<?> choices = (List<?>) json.get("choices");
            Map<?, ?> first = (Map<?, ?>) choices.get(0);
            Map<?, ?> msg = (Map<?, ?>) first.get("message");
            return (String) msg.get("content");

        } catch (Exception e) {
            e.printStackTrace();
            return "AI error: " + e.getMessage();
        }
    }
}
