package com.omar.postify.dto;

import java.time.LocalDateTime;

public record ConversationSummaryResponse(
        String publicId,
        Participant participant,
        MessagePreview latestMessage,
        LocalDateTime updatedAt
) {
    public record Participant(
            String publicId,
            String username,
            String avatarUrl
    ) {}

    public record MessagePreview(
            String content,
            String senderUsername,
            LocalDateTime createdAt
    ) {}
}
