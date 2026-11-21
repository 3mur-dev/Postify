package com.omar.postify.repository;

import com.omar.postify.entities.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
            select m from Message m
            where m.conversation.id = :conversationId
            order by m.createdAt desc, m.id desc
            """)
    Page<Message> findAllByConversationIdOrderByCreatedAtDesc(
            @Param("conversationId") Long conversationId,
            Pageable pageable
    );

    Optional<Message> findByConversation_IdAndSender_IdAndClientMessageId(
            Long conversationId,
            Long senderId,
            String clientMessageId
    );

    @Query("""
            select m from Message m
            where m.conversation.id in :conversationIds
              and not exists (
                  select newer.id from Message newer
                  where newer.conversation.id = m.conversation.id
                    and (
                        newer.createdAt > m.createdAt
                        or (newer.createdAt = m.createdAt and newer.id > m.id)
                    )
              )
            """)
    List<Message> findLatestByConversationIds(
            @Param("conversationIds") List<Long> conversationIds
    );

    @Modifying
    @Query("""
            update Message m
            set m.isRead = true,
                m.readAt = :readAt
            where m.conversation.id = :conversationId
              and m.sender.id <> :currentUserId
              and m.isRead = false
              and m.readAt is null
        """)
    int markMessagesAsReadWhereNotReadAndReadAtIsNullAndCurrentUserNotEqualToSender(
            @Param("conversationId") Long conversationId,
            @Param("currentUserId") Long currentUserId,
            @Param("readAt") LocalDateTime readAt
    );
}
