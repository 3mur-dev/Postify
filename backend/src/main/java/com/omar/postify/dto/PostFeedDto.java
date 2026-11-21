package com.omar.postify.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Flat DTO projection for the home feed.
 * All data is fetched in a single JPQL query using scalar subqueries
 * for like/comment counts and liked-by-user flag — zero N+1.
 */
public record PostFeedDto(
        Long id,
        String publicId,
        String content,
        List<PostMediaDto> media,
        LocalDateTime createdAt,
        String username,
        String avatarUrl,
        long likeCount,
        long commentCount,
        boolean likedByCurrentUser,
        boolean savedByCurrentUser
) {}