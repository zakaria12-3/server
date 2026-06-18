package com.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    @Value("${BREVO_API_KEY:}")
    private String apiKey;

    @Value("${SUPPORT_EMAIL:}")
    private String senderEmail;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private void sendEmail(String to, String subject, String htmlContent) {
        if (apiKey == null || apiKey.isBlank() || senderEmail == null || senderEmail.isBlank()) {
            throw new IllegalStateException("Email not sent because BREVO_API_KEY or SUPPORT_EMAIL is not configured");
        }

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
                throw new IllegalStateException("Brevo email error " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            LOGGER.error("Failed to send email to {}", to, e);
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            throw new IllegalStateException("Failed to send email: " + e.getMessage(), e);
        }
    }

    public void sendVerificationEmail(String to, String subject, String htmlContent) {
        sendEmail(to, subject, htmlContent);
    }

    public void sendPasswordResetEmail(String to, String subject, String htmlContent) {
        sendEmail(to, subject, htmlContent);
    }

    public void sendApplicationResultEmail(String to, String status, String jobTitle) {
        boolean accepted = "FINAL_ACCEPTED".equalsIgnoreCase(status) || "ACCEPTED".equalsIgnoreCase(status);
        String subject = "Your Job Application Status";
        String html = "<html><body style='font-family: Arial;'>"
                + "<h2>Application Update</h2>"
                + "<p>Your application for <b>" + jobTitle + "</b> has been <b>" + (accepted ? "accepted" : "rejected") + "</b>.</p>"
                + (accepted
                    ? "<p style='color:green;'>Congratulations! You have been selected!</p>"
                    : "<p style='color:red;'>We regret to inform you that you were not selected.</p>")
                + "</body></html>";
        try {
            sendEmail(to, subject, html);
        } catch (IllegalStateException e) {
            LOGGER.error("Application result email was not sent to {}", to, e);
        }
    }

    public void sendInterviewEmail(String to, String jobTitle, String link, java.time.LocalDateTime interviewDate) {
        String subject = "Interview Invitation";
        String dateText = interviewDate != null 
                ? "<p>Date & Time: <b>" + java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(interviewDate) + "</b></p>"
                : "";
        String html = "<h2>Interview Scheduled</h2>"
                + "<p>Your application for <b>" + jobTitle + "</b> was selected for an interview.</p>"
                + dateText
                + "<p>Join the meeting here:</p>"
                + "<a href='" + link + "'>" + link + "</a>";
        try {
            sendEmail(to, subject, html);
        } catch (IllegalStateException e) {
            LOGGER.error("Interview email was not sent to {}", to, e);
        }
    }

    public void sendCvAnalysisEmail(String to, String candidateName, String jobTitle, Integer score, String skillsFound) {
        String subject = "New CV analysis score for " + jobTitle;
        String skills = skillsFound == null || skillsFound.isBlank() ? "No matching skills found." : skillsFound;
        String displayedScore = score == null ? "N/A" : score + "%";
        String html = "<html><body style='font-family: Arial;'>"
                + "<h2>CV Analysis Complete</h2>"
                + "<p><b>" + candidateName + "</b> has applied for <b>" + jobTitle + "</b>.</p>"
                + "<p>AI match score: <b>" + displayedScore + "</b></p>"
                + "<p>Skills found: " + skills + "</p>"
                + "<p>Log in to StepUp to review the full application.</p>"
                + "</body></html>";
        try {
            sendEmail(to, subject, html);
        } catch (IllegalStateException e) {
            LOGGER.error("CV analysis email was not sent to {}", to, e);
        }
    }

    public void sendJobPostedEmail(String to, String jobTitle, String companyName, String description) {
        String subject = "New Job Opportunity: " + jobTitle + " at " + companyName;
        String preview = description == null || description.isBlank()
                ? "A new job listing is available now."
                : (description.length() > 200 ? description.substring(0, 200) + "..." : description);
        String html = "<html><body style='font-family: Arial;'>"
                + "<h2>New Job Posted!</h2>"
                + "<p>A new position for <b>" + jobTitle + "</b> at <b>" + companyName + "</b> has just been posted.</p>"
                + "<p><i>" + preview + "</i></p>"
                + "<p>Log in to StepUp to view more details and apply!</p>"
                + "</body></html>";
        try {
            sendEmail(to, subject, html);
        } catch (IllegalStateException e) {
            LOGGER.error("Job posted email was not sent to {}", to, e);
        }
    }
}
