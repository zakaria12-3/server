package com.example.dto;

import java.time.LocalDateTime;

public class MessageDto {

    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private LocalDateTime createdAt;
    private String senderName;

    public MessageDto() {}

    public MessageDto(Long id, Long senderId, Long receiverId,
                      String content, LocalDateTime createdAt, String senderName) {
        this.id         = id;
        this.senderId   = senderId;
        this.receiverId = receiverId;
        this.content    = content;
        this.createdAt  = createdAt;
        this.senderName = senderName;
    }


    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }

    public Long getSenderId()              { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getReceiverId()              { return receiverId; }
    public void setReceiverId(Long receiverId){ this.receiverId = receiverId; }

    public String getContent()             { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }

    public String getSenderName()              { return senderName; }
    public void setSenderName(String senderName){ this.senderName = senderName; }
}
