package com.omar.postify.dto;

public record AdminContext(
        Long adminId,
        String username,
        String ipAddress,
        String userAgent
) {}