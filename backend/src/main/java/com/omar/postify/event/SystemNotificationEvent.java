package com.omar.postify.event;

public record SystemNotificationEvent(
        String message,
        Long recipientId
)
{}
