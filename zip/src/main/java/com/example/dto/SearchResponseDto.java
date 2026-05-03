package com.example.dto;

import lombok.Data;
import java.util.List;

@Data
public class SearchResponseDto {
    private List<UserProfileDto> users;
    private List<JobDto> jobs;
    private List<PostDto> posts;
}
