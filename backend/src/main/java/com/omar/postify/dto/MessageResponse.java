package com.omar.postify.dto;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        String conversationPublicId,
        String senderPublicId,
        String senderUsername,
        String senderAvatarUrl,
        String content,
        String clientMessageId,
        LocalDateTime createdAt,
        boolean isRead,
        LocalDateTime readAt
) {}