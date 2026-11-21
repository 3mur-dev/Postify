package com.omar.postify.dto.admin;

import com.omar.postify.entities.enums.Role;
import com.omar.postify.entities.enums.UserStatus;

import java.time.LocalDateTime;

public record AdminUserDetailsDto(

        String publicId,

        String username,

        String email,

        UserStatus status,

        LocalDateTime createdAt,

        String bio,

        String avatarUrl,

        long followersCount,

        long followingCount,

        long postsCount,

        Role role

) {}