package com.omar.postify.dto;

public record UserSearchDto(
        Long id,
        String publicId,
        String username,
        String avatarUrl,
        String bio
) {
}