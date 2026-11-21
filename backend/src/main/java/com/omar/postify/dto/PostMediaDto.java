package com.omar.postify.dto;

import com.omar.postify.entities.enums.MediaType;

public record PostMediaDto(
        Long id,
        String url,
        MediaType type,
        int position,
        Long width,
        Long height,
        Long durationMs
) {}