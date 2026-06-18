package com.example.controller;

import com.example.dto.NotificationDto;
import com.example.dto.PlatformRatingDto;
import com.example.dto.UserProfileDto;
import com.example.model.User;
import com.example.service.NotificationService;
import com.example.service.PlatformRatingService;
import com.example.service.UserService;
import jakarta.validation.Valid;
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
    private final NotificationService notificationService;
    private final PlatformRatingService platformRatingService;

    public UserController(UserService userService, NotificationService notificationService, PlatformRatingService platformRatingService) {
        this.userService = userService;
        this.notificationService = notificationService;
        this.platformRatingService = platformRatingService;
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

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDto>> getMyNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getMyNotifications(authentication.getName()));
    }

    @GetMapping("/platform-rating")
    public ResponseEntity<PlatformRatingDto> getMyPlatformRating(Authentication authentication) {
        return ResponseEntity.ok(
                platformRatingService.getMyRating(authentication.getName()).orElseGet(PlatformRatingDto::new)
        );
    }

    @PutMapping("/platform-rating")
    public ResponseEntity<PlatformRatingDto> saveMyPlatformRating(@Valid @RequestBody PlatformRatingDto dto, Authentication authentication) {
        return ResponseEntity.ok(platformRatingService.saveMyRating(authentication.getName(), dto));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<java.util.Map<String, Long>> getUnreadNotificationsCount(Authentication authentication) {
        return ResponseEntity.ok(java.util.Map.of("count", notificationService.getUnreadCount(authentication.getName())));
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markNotificationAsRead(@PathVariable Long id, Authentication authentication) {
        notificationService.markAsRead(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllNotificationsAsRead(Authentication authentication) {
        notificationService.markAllAsRead(authentication.getName());
        return ResponseEntity.ok().build();
    }
}
