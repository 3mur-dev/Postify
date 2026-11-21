package com.omar.postify.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProfilePostDto(
        Long id,
        String publicId,
        String content,
        List<PostMediaDto> media,
        LocalDateTime createdAt,
        int likeCount,
        int commentCount,
        String username
) {}
