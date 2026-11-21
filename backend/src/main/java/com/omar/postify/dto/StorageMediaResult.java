package com.omar.postify.dto;

import com.omar.postify.entities.enums.MediaType;

public record StorageMediaResult(
        String url,
        MediaType type,
        Long width,
        Long height,
        Long durationMs
) {}