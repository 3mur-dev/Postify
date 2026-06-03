package com.omar.postify.dto;

import java.time.Instant;

public record PostDto(
        Long id,
        String content,
        String imageUrl,
        String username,
        String userAvatar,
        Instant createdAt
) {}