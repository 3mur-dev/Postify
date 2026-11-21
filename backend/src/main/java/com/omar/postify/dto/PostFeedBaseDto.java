package com.omar.postify.dto;

import java.time.LocalDateTime;

public record PostFeedBaseDto(
        Long id,
        String publicId,
        String content,
        LocalDateTime createdAt,
        String username,
        String avatarUrl
) {}
