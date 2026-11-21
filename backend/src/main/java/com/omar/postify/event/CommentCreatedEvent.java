package com.omar.postify.event;

public record CommentCreatedEvent(
        Long commentId,
        Long actorId
) {}
