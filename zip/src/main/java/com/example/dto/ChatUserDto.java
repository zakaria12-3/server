package com.example.dto;

import lombok.Data;

@Data
public class ChatUserDto {
    private Long id;
    private String username;
    private String role;
    private String avatarUrl;
    private String headline;
}
