package com.omar.postify.dto;

import com.omar.postify.entities.enums.NotificationType;

public record NotificationPayload(

        NotificationType type,
        String actorUsername,
        Long postId
) {}
