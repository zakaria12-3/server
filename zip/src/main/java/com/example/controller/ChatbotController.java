package com.example.controller;


import com.example.service.ChatbotService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    // POST /chat — body: { "message": "..." }
    // Role is derived from JWT, so one endpoint serves all user types
    @PostMapping
    public Map<String, String> chat(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String message = body.get("message");

        // Derive role from JWT authorities
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "").toLowerCase())
                .orElse("candidate");

        String reply = chatbotService.chat(role, message);
        return Map.of("reply", reply);
    }
}
