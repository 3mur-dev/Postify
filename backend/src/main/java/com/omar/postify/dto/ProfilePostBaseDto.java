package com.omar.postify.dto;

import java.time.LocalDateTime;

public record ProfilePostBaseDto(
        Long id,
        String publicId,
        String content,
        LocalDateTime createdAt,
        int likeCount,
        int commentCount,
        String username
) {}