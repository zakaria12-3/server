package com.example.repository;

import com.example.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
        SELECT m FROM Message m
        WHERE (m.sender.id = :userA AND m.receiver.id = :userB)
           OR (m.sender.id = :userB AND m.receiver.id = :userA)
        ORDER BY m.createdAt ASC
        """)
    List<Message> findConversation(@Param("userA") Long userA,
                                   @Param("userB") Long userB);

    @Query(value = """
        SELECT DISTINCT ON (conversation.partner_id)
            conversation.id,
            conversation.sender_id,
            conversation.receiver_id,
            conversation.content,
            conversation.created_at,
            conversation.read_at
        FROM (
            SELECT
                m.*,
                CASE
                    WHEN m.sender_id = :userId THEN m.receiver_id
                    ELSE m.sender_id
                END AS partner_id
            FROM messages m
            WHERE m.sender_id = :userId OR m.receiver_id = :userId
        ) conversation
        ORDER BY conversation.partner_id, conversation.created_at DESC, conversation.id DESC
        """, nativeQuery = true)
    List<Message> findLatestPerConversation(@Param("userId") Long userId);


    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.receiver.id = :receiverId
          AND m.sender.id   = :senderId
          AND m.readAt IS NULL
        """)
    long countUnread(@Param("receiverId") Long receiverId,
                     @Param("senderId")   Long senderId);

    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.receiver.id = :receiverId
          AND m.readAt IS NULL
        """)
    long countAllUnread(@Param("receiverId") Long receiverId);

    @Modifying
    @Query("""
        UPDATE Message m
        SET m.readAt = CURRENT_TIMESTAMP
        WHERE m.receiver.id = :receiverId
          AND m.sender.id   = :senderId
          AND m.readAt IS NULL
        """)
    void markAsRead(@Param("receiverId") Long receiverId,
                    @Param("senderId")   Long senderId);
}
