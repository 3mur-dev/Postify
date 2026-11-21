package com.omar.postify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PresenceRequest(
        @NotBlank
        @Size(max = 36)
        String conversationPublicId
) {}
