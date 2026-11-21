package com.omar.postify.dto;

import com.omar.postify.entities.enums.IdentityType;

public record RequestIdentity(
        IdentityType type,
        Long id
) {

    public static RequestIdentity authenticated(Long userId) {
        return new RequestIdentity(IdentityType.AUTHENTICATED, userId);
    }

    public static RequestIdentity anonymous(Long anonymousUserId) {
        return new RequestIdentity(IdentityType.ANONYMOUS, anonymousUserId);
    }

    public boolean isAuthenticated() {
        return type == IdentityType.AUTHENTICATED;
    }

    public boolean isAnonymous() {
        return type == IdentityType.ANONYMOUS;
    }
}
