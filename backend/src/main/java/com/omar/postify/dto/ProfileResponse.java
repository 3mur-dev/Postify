package com.omar.postify.dto;

import com.omar.postify.entities.enums.Role;

import java.util.List;

public record ProfileResponse(
        Long id,
        String publicId,
        String username,
        String bio,
        String avatarUrl,
        boolean ownProfile,
        boolean following,
        long followersCount,
        long followingCount,
        List<ProfilePostDto> posts,
        Role role
) {}
