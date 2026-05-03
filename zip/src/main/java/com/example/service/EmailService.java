package com.example.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Objects;


@Service

public class EmailService {
    private final JavaMailSender emailSender;


    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }


    void sendVerificationEmail(String to, String subject, String text) throws MessagingException{
        MimeMessage message= emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message,true);

        helper.setTo(Objects.requireNonNull(to));
        helper.setSubject(Objects.requireNonNull(subject));
        helper.setText(Objects.requireNonNull(text), true);

        emailSender.send(message);

    }
    public void sendApplicationResultEmail(String to, String status, String jobTitle) {

        String subject = "Your Job Application Status";

        String message = "<html>"
                + "<body style='font-family: Arial;'>"
                + "<h2>Application Update</h2>"
                + "<p>Your application for <b>" + jobTitle + "</b> has been <b>" + status + "</b>.</p>";

        if ("ACCEPTED".equalsIgnoreCase(status)) {
            message += "<p style='color:green;'>Congratulations! You have been selected !</p>";
        } else {
            message += "<p style='color:red;'>We regret to inform you that you were not selected.</p>";
        }

        message += "</body></html>";

        try {
            sendVerificationEmail(to, subject, message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
    public void sendInterviewEmail(String to, String jobTitle, String link) {

        String subject = "Interview Invitation";

        String message = "<h2>Interview Scheduled</h2>"
                + "<p>Your application for <b>" + jobTitle + "</b> was accepted.</p>"
                + "<p>Join meeting here:</p>"
                + "<a href='" + link + "'>" + link + "</a>";

        try {
            sendVerificationEmail(to, subject, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}
