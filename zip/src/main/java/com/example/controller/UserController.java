package com.example.controller;

import com.example.dto.UserProfileDto;
import com.example.model.User;
import com.example.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/users")
@RestController
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> authenticatedUser(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(userService.getUserProfile(user.getId()));
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> allUsers() {
        List<User> users = userService.allUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<UserProfileDto> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserProfile(id));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileDto> updateMyProfile(@RequestBody UserProfileDto dto, Authentication authentication) {
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        UserProfileDto updatedProfile = userService.updateUserProfile(user.getId(), dto);
        return ResponseEntity.ok(updatedProfile);
    }
}
