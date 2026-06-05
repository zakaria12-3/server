package com.example.dto;

import lombok.Data;

@Data
public class UserProfileDto {
    private Long id;
    private String username;
    private String email;
    private String bio;
    private String headline;
    private String location;
    private String avatarUrl;
    private String companyName;
    private boolean reported;
    private boolean suspended;
}
