package com.omar.postify.dto;

public record PresenceResponse(
        String conversationPublicId,
        String participantPublicId,
        boolean online
) {}
