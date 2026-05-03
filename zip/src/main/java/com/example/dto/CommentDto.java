package com.example.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentDto {
    private Long id;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private String content;
    private LocalDateTime createdAt;
}
