package com.example.service;

import com.example.dto.GoogleAuthDto;
import com.example.dto.LoginUserDto;
import com.example.dto.RegisterUserDto;
import com.example.dto.VerifyUserDto;
import com.example.model.Role;
import com.example.model.User;
import com.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final com.example.repository.CompanyRepository companyRepository;

    @Value("${google.client-id}")
    private String googleClientId;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            EmailService emailService,
            com.example.repository.CompanyRepository companyRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.companyRepository = companyRepository;
    }

    public User signup(RegisterUserDto input) {
        if (userRepository.findByEmail(input.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.findByUsername(input.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User(input.getUsername(), input.getEmail(), passwordEncoder.encode(input.getPassword()));
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(7));
        user.setEnabled(false);

        String role = input.getRole();
        if ("RECRUITER".equalsIgnoreCase(role)) {
            user.setRole(Role.ROLE_RECRUITER);
            if (input.getCompanyName() != null && !input.getCompanyName().trim().isEmpty()) {
                com.example.model.Company company = companyRepository.findByName(input.getCompanyName())
                        .orElseGet(() -> {
                            com.example.model.Company newCompany = new com.example.model.Company(input.getCompanyName());
                            return companyRepository.save(newCompany);
                        });
                user.setCompany(company);
            }
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            user.setRole(Role.ROLE_ADMIN);
        } else if ("CANDIDATE".equalsIgnoreCase(role)) {
            user.setRole(Role.ROLE_CANDIDATE);
        } else {
            throw new RuntimeException("Invalid role provided");
        }

        User savedUser = userRepository.save(user);
        sendVerificationEmail(savedUser);
        return savedUser;
    }

    public User authenticate(LoginUserDto input) {
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEnabled()) {
            throw new RuntimeException("Account not verified");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(input.getEmail(), input.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid email or password");
        }

        return user;
    }

    public User authenticateWithGoogle(GoogleAuthDto input) {
        if (input.getIdToken() == null || input.getIdToken().isBlank()) {
            throw new RuntimeException("Google token is required");
        }

        Map<String, Object> googleUser = verifyGoogleToken(input.getIdToken());
        String email = String.valueOf(googleUser.get("email")).toLowerCase();
        String fallbackName = email.substring(0, email.indexOf("@"));
        String name = String.valueOf(googleUser.getOrDefault("name", fallbackName));
        String picture = String.valueOf(googleUser.getOrDefault("picture", ""));

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User(uniqueUsername(name, email), email, passwordEncoder.encode(generateRandomPassword()));
            newUser.setRole(resolveRole(input.getRole()));
            newUser.setEnabled(true);
            return newUser;
        });

        user.setEnabled(true);
        if (user.getRole() == null) {
            user.setRole(resolveRole(input.getRole()));
        }
        if (!picture.isBlank()) {
            user.setAvatarUrl(picture);
        }

        return userRepository.save(user);
    }

    private Map<String, Object> verifyGoogleToken(String idToken) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new RuntimeException("Google client id is not configured");
        }

        RestTemplate restTemplate = new RestTemplate();
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(
                "https://oauth2.googleapis.com/tokeninfo?id_token={idToken}",
                Map.class,
                idToken
        );

        if (response == null || response.get("email") == null) {
            throw new RuntimeException("Invalid Google token");
        }
        if (!googleClientId.equals(response.get("aud"))) {
            throw new RuntimeException("Google token audience does not match this app");
        }
        if (!"true".equals(String.valueOf(response.get("email_verified")))) {
            throw new RuntimeException("Google email is not verified");
        }

        return response;
    }

    private Role resolveRole(String role) {
        if ("RECRUITER".equalsIgnoreCase(role)) {
            return Role.ROLE_RECRUITER;
        }
        if ("ADMIN".equalsIgnoreCase(role)) {
            return Role.ROLE_ADMIN;
        }
        return Role.ROLE_CANDIDATE;
    }

    private String uniqueUsername(String name, String email) {
        String base = name == null || name.isBlank() ? email.substring(0, email.indexOf("@")) : name;
        base = base.trim().replaceAll("[^A-Za-z0-9_]", "_");
        if (base.isBlank()) {
            base = "google_user";
        }

        String username = base;
        int suffix = 1;
        while (userRepository.findByUsername(username).isPresent()) {
            username = base + suffix;
            suffix++;
        }
        return username;
    }

    private String generateRandomPassword() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public void verifyUser(VerifyUserDto input) {
        Optional<User> optionalUser = userRepository.findByEmail(input.getEmail());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Verification code has expired !");
            }
            if (user.getVerificationCode().equals(input.getVerificationCode())) {
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationCodeExpiresAt(null);
                userRepository.save(user);
            } else {
                throw new RuntimeException("Invalid verification code !");
            }
        } else {
            throw new RuntimeException("User not found !");
        }
    }

    public void resendVerificationCode(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isEnabled()) {
                throw new RuntimeException("Account is already verified !");
            }
            user.setVerificationCode(generateVerificationCode());
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(1));
            sendVerificationEmail(user);
            userRepository.save(user);
        } else {
            throw new RuntimeException("User not found !");
        }
    }

    public void sendVerificationEmail(User user) {
        String subject = "Account Verification !";
        String verificationCode = user.getVerificationCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to our app!</h2>"
                + "<p style=\"font-size: 16px;\">Please enter the verification code below to continue:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        CompletableFuture.runAsync(() -> {
            try {
                emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String generateVerificationCode() {
        Random random = new SecureRandom();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
