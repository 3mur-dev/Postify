package com.omar.postify.event;

public record UserFollowedEvent(
        Long followerId,
        Long followingId
) {}
