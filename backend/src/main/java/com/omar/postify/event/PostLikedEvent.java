package com.omar.postify.event;

public record PostLikedEvent(
        Long postId,
        Long actorId
) {}