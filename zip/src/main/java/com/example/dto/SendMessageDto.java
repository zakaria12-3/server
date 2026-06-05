package com.example.dto;

import lombok.Data;

@Data
public class SendMessageDto {
    private Long receiverId;
    private String content;
}
