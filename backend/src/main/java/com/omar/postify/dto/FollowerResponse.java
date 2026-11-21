package com.omar.postify.dto;

import lombok.Builder;

@Builder
public record FollowerResponse(

        Long id,
        String username,
        String avatarUrl,
        boolean followedByCurrentUser

) {}