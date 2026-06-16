package com.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;   // null = unread


    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }

    public User getSender()                { return sender; }
    public void setSender(User sender)     { this.sender = sender; }

    public User getReceiver()              { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }

    public String getContent()             { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }

    public LocalDateTime getReadAt()                 { return readAt; }
    public void setReadAt(LocalDateTime readAt)      { this.readAt = readAt; }
}
