package com.example.dto;

import java.time.LocalDateTime;


public class ConversationDto {

    private Long userId;
    private String username;
    private String avatarUrl;
    private String role;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;

    public ConversationDto() {}


    public Long getUserId()              { return userId; }
    public void setUserId(Long userId)   { this.userId = userId; }

    public String getUsername()              { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatarUrl()               { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getRole()            { return role; }
    public void setRole(String role)   { this.role = role; }

    public String getLastMessage()                 { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public LocalDateTime getLastMessageAt()                    { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt)  { this.lastMessageAt = lastMessageAt; }

    public long getUnreadCount()             { return unreadCount; }
    public void setUnreadCount(long unreadCount){ this.unreadCount = unreadCount; }
}
