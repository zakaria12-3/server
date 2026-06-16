package com.example.controller;

import com.example.model.User;
import com.example.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserSearchController {

    private final UserRepository userRepo;

    public UserSearchController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(
            @RequestParam String q,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.badRequest().build();
        }

        Long callerId = userRepo.findByEmail(userDetails.getUsername())
                .map(User::getId)
                .orElse(-1L);

        List<User> results = userRepo.searchByUsernameOrEmail(q.trim());

        List<Map<String, Object>> response = results.stream()
                .filter(u -> !u.getId().equals(callerId))
                .limit(20)
                .map(u -> Map.<String, Object>of(
                        "id",        u.getId(),
                        "username",  u.getRealUsername(),
                        "avatarUrl", u.getAvatarUrl() != null ? u.getAvatarUrl() : ""
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
