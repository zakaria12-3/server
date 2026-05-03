package com.example.service;

import com.example.dto.LoginUserDto;
import com.example.dto.RegisterUserDto;
import com.example.dto.VerifyUserDto;
import com.example.model.Role;
import com.example.model.User;
import com.example.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final EmailService  emailService;

    private final com.example.repository.CompanyRepository companyRepository;


    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            EmailService emailService,
            com.example.repository.CompanyRepository companyRepository
    ){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
        this.authenticationManager=authenticationManager;
        this.emailService=emailService;
        this.companyRepository=companyRepository;
    }
    public User signup(RegisterUserDto input){
        if (userRepository.findByEmail(input.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        if (userRepository.findByUsername(input.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user= new User(input.getUsername(),input.getEmail(),passwordEncoder.encode(input.getPassword()));
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
                    new UsernamePasswordAuthenticationToken(
                            input.getEmail(),
                            input.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid email or password");
        }

        return user;
    }public void verifyUser(VerifyUserDto input){
        Optional<User> optionalUser= userRepository.findByEmail(input.getEmail());
        if(optionalUser.isPresent()){
            User user = optionalUser.get();
            if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())){
                throw new RuntimeException("Verification code has expired !");
            }
            if(user.getVerificationCode().equals(input.getVerificationCode())){
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationCodeExpiresAt(null);
                userRepository.save(user);

            }else{
                throw new RuntimeException("Invalid verification code ! ");}
            }else{
                throw new RuntimeException("User not found !");
            }
        }
        public void resendVerificationCode(String email){
            Optional<User> optionalUser= userRepository.findByEmail(email);
            if(optionalUser.isPresent()) {
                User user = optionalUser.get();
                if (user.isEnabled()) {

                    throw new RuntimeException("Account is already verified !");

                }
                user.setVerificationCode(generateVerificationCode());
                user.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(1));
                sendVerificationEmail(user);
                userRepository.save(user);
            }else{
                throw new RuntimeException("User not found !");

            }
        }
        public void sendVerificationEmail(User user){
        String subject="Account Verification !";
        String verificationCode=user.getVerificationCode();
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
                try{
                    emailService.sendVerificationEmail(user.getEmail(),subject,htmlMessage);
                } catch (MessagingException e){
                    e.printStackTrace();
                }
            });


        }



    private String generateVerificationCode(){
            Random random= new SecureRandom();


            int code=random.nextInt(900000) + 100000;
            return String.valueOf(code);


    }










}
