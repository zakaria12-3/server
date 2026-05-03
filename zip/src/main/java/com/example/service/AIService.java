package com.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Service
public class AIService {

    private final String OLLAMA_URL = "http://localhost:11434/api/generate";

    public String askAI(String prompt) {
        return askAI(prompt, false);
    }

    public String askAI(String prompt, boolean requireJson) {
        try {
            URL url = new URL(OLLAMA_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("model", "qwen2:1.5b");
            bodyMap.put("prompt", prompt);
            bodyMap.put("stream", false);
            if (requireJson) {
                bodyMap.put("format", "json");
            }

            String jsonInput = mapper.writeValueAsString(bodyMap);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes());
                os.flush();
            }
            int status = conn.getResponseCode();

            BufferedReader br;
            if (status >= 200 && status < 300) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            System.out.println("OLLAMA RAW RESPONSE: " + response);
            Map<String, Object> json = mapper.readValue(response.toString(), Map.class);



            return (String) json.getOrDefault("response", "AI error");

        } catch (Exception e) {
            e.printStackTrace();
            return "AI error";
        }
    }
}