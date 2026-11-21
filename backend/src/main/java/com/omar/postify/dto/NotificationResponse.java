package com.omar.postify.dto;

import com.omar.postify.entities.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        String publicId,
        String message,
        NotificationType type,
        String actorPublicId,
        String actorUsername,
        String postPublicId,
        Long commentId,
        boolean isRead,
        LocalDateTime createdAt
) {}
