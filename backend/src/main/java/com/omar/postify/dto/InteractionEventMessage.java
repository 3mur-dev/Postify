package com.omar.postify.dto;

import com.omar.postify.entities.enums.InteractionEventType;
import com.omar.postify.entities.enums.InteractionSource;

import java.time.Instant;

public record InteractionEventMessage(

        RequestIdentity  identity,

        Long postId,

        InteractionEventType eventType,

        Instant occurredAt,

        String sessionId,

        String clientEventId,

        Long positionMs,

        Long durationMs,

        Double completionRatio,

        InteractionSource source,

        String metadata,

        Integer eventVersion

) {
}