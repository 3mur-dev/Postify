package com.omar.postify.dto;

import com.omar.postify.entities.enums.UserStatus;

public record UpdateUserStatusRequest(
        UserStatus status
) {
}
