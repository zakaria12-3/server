package com.example.service;

import com.example.dto.ConversationDto;
import com.example.dto.MessageDto;
import com.example.dto.SendMessageDto;
import com.example.model.Message;
import com.example.model.User;
import com.example.repository.MessageRepository;
import com.example.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    public MessageDto sendMessage(Long senderId, SendMessageDto dto) {
        User sender = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findById(dto.getReceiverId()).orElseThrow(() -> new RuntimeException("Receiver not found"));

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(dto.getContent());
        message.setCreatedAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);
        return mapToDto(savedMessage);
    }

    public List<MessageDto> getConversationMessages(Long currentUserId, Long otherUserId) {
        List<Message> messages = messageRepository.findMessagesBetweenUsers(currentUserId, otherUserId);
        return messages.stream().map(this::mapToDto).toList();
    }

    public List<ConversationDto> getUserConversations(Long userId) {
        List<Message> allMessages = messageRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        
        Map<Long, ConversationDto> conversationMap = new LinkedHashMap<>();

        for (Message m : allMessages) {
            Long otherUserId = m.getSender().getId().equals(userId) ? m.getReceiver().getId() : m.getSender().getId();
            User otherUser = m.getSender().getId().equals(userId) ? m.getReceiver() : m.getSender();

            if (!conversationMap.containsKey(otherUserId)) {
                ConversationDto dto = new ConversationDto();
                dto.setUserId(otherUserId);
                dto.setUsername(otherUser.getUsername() != null ? otherUser.getUsername() : otherUser.getCompany());
                dto.setRole(otherUser.getRole().getName());
                dto.setLastMessage(m.getContent());
                dto.setLastMessageAt(m.getCreatedAt().toString());
                dto.setUnreadCount(0); // Simplified for now
                conversationMap.put(otherUserId, dto);
            }
        }

        return new ArrayList<>(conversationMap.values());
    }

    private MessageDto mapToDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setSenderId(message.getSender().getId());
        dto.setReceiverId(message.getReceiver().getId());
        dto.setContent(message.getContent());
        dto.setCreatedAt(message.getCreatedAt().toString());
        dto.setSenderName(message.getSender().getUsername() != null ? message.getSender().getUsername() : message.getSender().getCompany());
        return dto;
    }
}
