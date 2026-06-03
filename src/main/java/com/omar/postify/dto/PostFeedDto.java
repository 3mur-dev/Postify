package com.omar.postify.dto;

import java.time.LocalDateTime;

public record PostFeedDto(
        Long id,
        String content,
        String imageUrl,
        Long authorId,
        String username,
        String avatarUrl,
        LocalDateTime createdAt,
        long likeCount,
        long commentCount,
        boolean likedByCurrentUser
) {
    public PostFeedDto withStats(long likeCount, long commentCount, boolean likedByCurrentUser) {
        return new PostFeedDto(
                id,
                content,
                imageUrl,
                authorId,
                username,
                avatarUrl,
                createdAt,
                likeCount,
                commentCount,
                likedByCurrentUser
        );
    }
}
