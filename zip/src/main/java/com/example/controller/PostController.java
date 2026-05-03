package com.example.controller;

import com.example.dto.PostDto;
import com.example.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<PostDto> createPost(@RequestBody Map<String, String> payload, Authentication authentication) {
        String email = authentication.getName();
        String content = payload.get("content");
        PostDto postDto = postService.createPost(email, content);
        return ResponseEntity.ok(postDto);
    }

    @GetMapping
    public ResponseEntity<List<PostDto>> getAllPosts(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(postService.getAllPosts(email));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<PostDto> toggleLike(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(postService.toggleLike(id, email));
    }

    @PostMapping("/{id}/comment")
    public ResponseEntity<PostDto> addComment(@PathVariable Long id, @RequestBody Map<String, String> payload, Authentication authentication) {
        String email = authentication.getName();
        String content = payload.get("content");
        return ResponseEntity.ok(postService.addComment(id, email, content));
    }
}
