package com.omar.postify.dto;

import java.time.LocalDateTime;

public record CommentDto(
        Long id,
        String content,
        LocalDateTime createdAt,

        String userPublicId,
        String username,
        String userAvatarUrl,

        String postPublicId
) {}