package com.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${BREVO_API_KEY}")
    private String apiKey;

    @Value("${SUPPORT_EMAIL}")
    private String senderEmail;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            Map<String, Object> body = Map.of(
                "sender",      Map.of("email", senderEmail),
                "to",          List.of(Map.of("email", to)),
                "subject",     subject,
                "htmlContent", htmlContent
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("Content-Type", "application/json")
                .header("api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 300) {
                System.err.println("Brevo email error " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendVerificationEmail(String to, String subject, String htmlContent) {
        sendEmail(to, subject, htmlContent);
    }

    public void sendApplicationResultEmail(String to, String status, String jobTitle) {
        String subject = "Your Job Application Status";
        String html = "<html><body style='font-family: Arial;'>"
                + "<h2>Application Update</h2>"
                + "<p>Your application for <b>" + jobTitle + "</b> has been <b>" + status + "</b>.</p>"
                + ("ACCEPTED".equalsIgnoreCase(status)
                    ? "<p style='color:green;'>Congratulations! You have been selected!</p>"
                    : "<p style='color:red;'>We regret to inform you that you were not selected.</p>")
                + "</body></html>";
        sendEmail(to, subject, html);
    }

    public void sendInterviewEmail(String to, String jobTitle, String link) {
        String subject = "Interview Invitation";
        String html = "<h2>Interview Scheduled</h2>"
                + "<p>Your application for <b>" + jobTitle + "</b> was accepted.</p>"
                + "<p>Join the meeting here:</p>"
                + "<a href='" + link + "'>" + link + "</a>";
        sendEmail(to, subject, html);
    }
}
