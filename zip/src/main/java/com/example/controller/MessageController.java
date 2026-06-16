package com.example.controller;

import com.example.dto.ConversationDto;
import com.example.dto.MessageDto;
import com.example.dto.SendMessageRequest;
import com.example.service.MessagingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessagingService messagingService;
    private final com.example.repository.UserRepository userRepo;

    public MessageController(MessagingService messagingService,
                             com.example.repository.UserRepository userRepo) {
        this.messagingService = messagingService;
        this.userRepo = userRepo;
    }


    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDto>> getConversations(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = resolveUserId(userDetails);
        return ResponseEntity.ok(messagingService.getConversations(userId));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails);
        return ResponseEntity.ok(Map.of("id", userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails);
        return ResponseEntity.ok(Map.of("count", messagingService.getUnreadTotal(userId)));
    }

    @GetMapping("/{partnerId}")
    public ResponseEntity<List<MessageDto>> getMessages(
            @PathVariable Long partnerId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = resolveUserId(userDetails);
        return ResponseEntity.ok(messagingService.getConversation(userId, partnerId));
    }

    @PostMapping("/send")
    public ResponseEntity<MessageDto> send(
            @RequestBody SendMessageRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long senderId = resolveUserId(userDetails);

        if (req.getReceiverId() == null || req.getContent() == null || req.getContent().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        MessageDto sent = messagingService.send(senderId, req.getReceiverId(), req.getContent());
        return ResponseEntity.ok(sent);
    }

    @GetMapping("/block-status/{partnerId}")
    public ResponseEntity<Map<String, Boolean>> blockStatus(
            @PathVariable Long partnerId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = resolveUserId(userDetails);
        return ResponseEntity.ok(Map.of(
                "blockedByMe", messagingService.hasBlocked(userId, partnerId),
                "blockedMe", messagingService.hasBlocked(partnerId, userId)
        ));
    }

    @PostMapping("/block/{partnerId}")
    public ResponseEntity<?> block(
            @PathVariable Long partnerId,
            @AuthenticationPrincipal UserDetails userDetails) {

        messagingService.blockUser(resolveUserId(userDetails), partnerId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/block/{partnerId}")
    public ResponseEntity<?> unblock(
            @PathVariable Long partnerId,
            @AuthenticationPrincipal UserDetails userDetails) {

        messagingService.unblockUser(resolveUserId(userDetails), partnerId);
        return ResponseEntity.ok().build();
    }


    private Long resolveUserId(UserDetails userDetails) {
        if (userDetails instanceof com.example.model.User u) {
            return u.getId();
        }

        return userRepo.findByEmail(userDetails.getUsername())
                .map(com.example.model.User::getId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
    }
}
