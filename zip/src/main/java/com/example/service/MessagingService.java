package com.example.service;

import com.example.dto.ConversationDto;
import com.example.dto.MessageDto;
import com.example.model.Message;
import com.example.model.MessageBlock;
import com.example.model.User;
import com.example.repository.MessageBlockRepository;
import com.example.repository.MessageRepository;
import com.example.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessagingService {

    private final MessageRepository messageRepo;
    private final MessageBlockRepository blockRepo;
    private final UserRepository    userRepo;

    public MessagingService(MessageRepository messageRepo,
                            MessageBlockRepository blockRepo,
                            UserRepository    userRepo) {
        this.messageRepo = messageRepo;
        this.blockRepo = blockRepo;
        this.userRepo    = userRepo;
    }


    @Transactional
    public MessageDto send(Long senderId, Long receiverId, String content) {
        User sender   = userRepo.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepo.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        if (isBlockedEitherWay(senderId, receiverId)) {
            throw new IllegalStateException("This conversation is blocked");
        }

        Message msg = new Message();
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setContent(content.trim());

        Message saved = messageRepo.save(msg);
        return toDto(saved);
    }


    @Transactional
    public List<MessageDto> getConversation(Long currentUserId, Long partnerId) {
        messageRepo.markAsRead(currentUserId, partnerId);

        return messageRepo.findConversation(currentUserId, partnerId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }


    public List<ConversationDto> getConversations(Long userId) {
        List<Message> latestMessages = messageRepo.findLatestPerConversation(userId);

        return latestMessages.stream().map(msg -> {
            User partner = msg.getSender().getId().equals(userId)
                    ? msg.getReceiver()
                    : msg.getSender();

            long unread = messageRepo.countUnread(userId, partner.getId());

            ConversationDto dto = new ConversationDto();
            dto.setUserId(partner.getId());
            dto.setUsername(partner.getRealUsername());
            dto.setAvatarUrl(partner.getAvatarUrl());
            dto.setRole(partner.getRole() != null ? partner.getRole().name() : "");
            dto.setLastMessage(msg.getContent());
            dto.setLastMessageAt(msg.getCreatedAt());
            dto.setUnreadCount(unread);
            return dto;
        }).collect(Collectors.toList());
    }

    public long getUnreadTotal(Long userId) {
        return messageRepo.countAllUnread(userId);
    }

    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("You cannot block yourself");
        }
        if (blockRepo.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            return;
        }
        User blocker = userRepo.findById(blockerId)
                .orElseThrow(() -> new RuntimeException("Blocker not found"));
        User blocked = userRepo.findById(blockedId)
                .orElseThrow(() -> new RuntimeException("Blocked user not found"));

        MessageBlock block = new MessageBlock();
        block.setBlocker(blocker);
        block.setBlocked(blocked);
        blockRepo.save(block);
    }

    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        blockRepo.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    public boolean hasBlocked(Long blockerId, Long blockedId) {
        return blockRepo.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    public boolean isBlockedEitherWay(Long userA, Long userB) {
        return blockRepo.existsByBlockerIdAndBlockedId(userA, userB)
                || blockRepo.existsByBlockerIdAndBlockedId(userB, userA);
    }


    private MessageDto toDto(Message m) {
        return new MessageDto(
                m.getId(),
                m.getSender().getId(),
                m.getReceiver().getId(),
                m.getContent(),
                m.getCreatedAt(),
                m.getSender().getRealUsername()
        );
    }
}
