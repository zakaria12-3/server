package com.example.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostDto {
    private Long id;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private String content;
    private LocalDateTime createdAt;
    private int likesCount;
    private boolean likedByCurrentUser;
    private List<CommentDto> comments;
}
