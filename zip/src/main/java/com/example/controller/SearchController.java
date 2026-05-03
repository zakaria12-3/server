package com.example.controller;

import com.example.dto.SearchResponseDto;
import com.example.model.User;
import com.example.repository.UserRepository;
import com.example.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;
    private final UserRepository userRepository;

    public SearchController(SearchService searchService, UserRepository userRepository) {
        this.searchService = searchService;
        this.userRepository = userRepository;
    }
    @GetMapping
    public ResponseEntity<SearchResponseDto> search(@RequestParam String q, Authentication authentication) {
        Long currentUserId = null;
        if (authentication != null && authentication.getName() != null) {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user != null) {
                currentUserId = user.getId();
            }
        }
        return ResponseEntity.ok(searchService.globalSearch(q, currentUserId));
    }
}
