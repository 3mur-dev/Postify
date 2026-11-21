package com.omar.postify.dto.admin;

import com.omar.postify.entities.enums.UserStatus;

import java.time.LocalDateTime;

public record AdminUserDto(

        String publicId,

        String username,

        String email,

        UserStatus status,

        LocalDateTime createdAt



) {}