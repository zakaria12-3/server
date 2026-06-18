package com.example.service;

import com.example.dto.LoginUserDto;
import com.example.dto.RegisterUserDto;
import com.example.dto.VerifyUserDto;
import com.example.model.Role;
import com.example.model.User;
import com.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthenticationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final com.example.repository.CompanyRepository companyRepository;

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

    @Transactional
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
        user.setEmailVerified(false);
        user.setPhone(input.getRecruiterPhone());
        user.setJobTitle(input.getRecruiterJobTitle());

        String role = input.getRole();
        if ("RECRUITER".equalsIgnoreCase(role)) {
            user.setRole(Role.ROLE_RECRUITER);
            user.setApprovalStatus("PENDING");
            if (input.getCompanyName() != null && !input.getCompanyName().trim().isEmpty()) {
                com.example.model.Company company = companyRepository.findByName(input.getCompanyName())
                        .orElseGet(() -> {
                            com.example.model.Company newCompany = new com.example.model.Company(input.getCompanyName());
                            return companyRepository.save(newCompany);
                        });
                applyCompanyDetails(company, input);
                user.setCompany(company);
            }
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            user.setRole(Role.ROLE_ADMIN);
            user.setApprovalStatus("APPROVED");
        } else if ("CANDIDATE".equalsIgnoreCase(role)) {
            user.setRole(Role.ROLE_CANDIDATE);
            user.setApprovalStatus("APPROVED");
        } else {
            throw new RuntimeException("Invalid role provided");
        }

        User savedUser = userRepository.save(user);
        sendVerificationEmail(savedUser);
        return savedUser;
    }

    private void applyCompanyDetails(com.example.model.Company company, RegisterUserDto input) {
        company.setLegalName(input.getCompanyLegalName());
        company.setRegistrationNumber(input.getRegistrationNumber());
        company.setTaxIdentifier(input.getTaxIdentifier());
        company.setIndustry(input.getIndustry());
        company.setCompanySize(input.getCompanySize());
        company.setAddress(input.getCompanyAddress());
        company.setCity(input.getCompanyCity());
        company.setCountry(input.getCompanyCountry());
        company.setWebsite(input.getCompanyWebsite());
        company.setPhone(input.getCompanyPhone());
        company.setProfessionalEmail(input.getCompanyEmail());
        companyRepository.save(company);
    }

    public User authenticate(LoginUserDto input) {
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEmailVerified()) {
            throw new RuntimeException("Account not verified");
        }
        if (user.getRole() == Role.ROLE_RECRUITER && "PENDING".equalsIgnoreCase(user.getApprovalStatus())) {
            throw new RuntimeException("Recruiter account is waiting for admin approval");
        }
        if (user.getRole() == Role.ROLE_RECRUITER && "REJECTED".equalsIgnoreCase(user.getApprovalStatus())) {
            throw new RuntimeException("Recruiter account was rejected by the admin");
        }
        if (!user.isEnabled()) {
            throw new RuntimeException("Account is disabled");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(input.getEmail(), input.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid email or password");
        }

        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public void verifyUser(VerifyUserDto input) {
        Optional<User> optionalUser = userRepository.findByEmail(input.getEmail());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isEmailVerified() && user.getVerificationCode() == null) {
                return;
            }
            if (user.getVerificationCodeExpiresAt() == null || user.getVerificationCode() == null) {
                throw new RuntimeException("No active verification code. Please request a new code.");
            }
            if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Verification code has expired !");
            }
            if (user.getVerificationCode().equals(input.getVerificationCode())) {
                user.setEmailVerified(true);
                if (user.getRole() == Role.ROLE_RECRUITER) {
                    user.setEnabled(false);
                    if (!"REJECTED".equalsIgnoreCase(user.getApprovalStatus())) {
                        user.setApprovalStatus("PENDING");
                    }
                } else {
                    user.setEnabled(true);
                    user.setApprovalStatus("APPROVED");
                }
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
            if (user.isEmailVerified()) {
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

    @Transactional
    public void requestPasswordReset(String email) {
        User user = findUserByEmail(email);
        user.setPasswordResetCode(generateVerificationCode());
        user.setPasswordResetCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);
        sendPasswordResetEmail(user);
    }

    public void verifyPasswordResetCode(String email, String code) {
        User user = findUserByEmail(email);
        validatePasswordResetCode(user, code);
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        User user = findUserByEmail(email);
        validatePasswordResetCode(user, code);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetCode(null);
        user.setPasswordResetCodeExpiresAt(null);
        userRepository.save(user);
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

        emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        LOGGER.info("Verification email sent to {}", user.getEmail());
    }

    public void sendPasswordResetEmail(User user) {
        String subject = "Password Reset Code";
        String resetCode = user.getPasswordResetCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Reset your StepUp password</h2>"
                + "<p style=\"font-size: 16px;\">Use the code below to change your password. This code expires in 15 minutes.</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Reset Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + resetCode + "</p>"
                + "</div>"
                + "<p style=\"font-size: 13px; color: #555;\">If you did not request this, you can ignore this email.</p>"
                + "</div>"
                + "</body>"
                + "</html>";

        emailService.sendPasswordResetEmail(user.getEmail(), subject, htmlMessage);
        LOGGER.info("Password reset email sent to {}", user.getEmail());
    }

    private User findUserByEmail(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found !"));
    }

    private void validatePasswordResetCode(User user, String code) {
        if (user.getPasswordResetCode() == null || user.getPasswordResetCodeExpiresAt() == null) {
            throw new RuntimeException("No active password reset code. Please request a new code.");
        }
        if (user.getPasswordResetCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Password reset code has expired !");
        }
        if (!user.getPasswordResetCode().equals(String.valueOf(code))) {
            throw new RuntimeException("Invalid password reset code !");
        }
    }

    private String generateVerificationCode() {
        Random random = new SecureRandom();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
