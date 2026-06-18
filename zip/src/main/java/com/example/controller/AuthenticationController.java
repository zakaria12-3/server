package com.example.controller;

import com.example.dto.ForgotPasswordRequestDto;
import com.example.dto.LoginUserDto;
import com.example.dto.RegisterUserDto;
import com.example.dto.ResetPasswordDto;
import com.example.dto.VerifyPasswordResetCodeDto;
import com.example.dto.VerifyUserDto;
import com.example.model.User;
import com.example.response.LoginResponse;
import com.example.service.AuthenticationService;
import com.example.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;

    public AuthenticationController(AuthenticationService authenticationService,
                                    JwtService jwtService) {
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
    }

    @PostMapping("/signup")
    public ResponseEntity<User> signup(@Valid @RequestBody RegisterUserDto dto) {

        User user = authenticationService.signup(dto);

        return ResponseEntity.status(201).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginUserDto dto) {

        User user = authenticationService.authenticate(dto);

        String token = jwtService.generateToken(user);;

        return ResponseEntity.ok(
                new LoginResponse(token, jwtService.getExpirationTime())
        );
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verify(@RequestBody VerifyUserDto dto) {

        authenticationService.verifyUser(dto);

        return ResponseEntity.ok("Account verified!");

    }

    @PostMapping("/resend")
    public void resend(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        authenticationService.resendVerificationCode(email);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto dto) {
        authenticationService.requestPasswordReset(dto.getEmail());
        return ResponseEntity.ok("Password reset code sent.");
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<String> verifyPasswordResetCode(@Valid @RequestBody VerifyPasswordResetCodeDto dto) {
        authenticationService.verifyPasswordResetCode(dto.getEmail(), dto.getCode());
        return ResponseEntity.ok("Password reset code verified.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordDto dto) {
        authenticationService.resetPassword(dto.getEmail(), dto.getCode(), dto.getNewPassword());
        return ResponseEntity.ok("Password changed successfully.");
    }
}
