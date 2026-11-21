package com.omar.postify.entities;

import com.omar.postify.entities.enums.Role;
import com.omar.postify.entities.enums.UserStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomUserDetailsTest {

    @Test
    void enablesOnlyActiveAccounts() {
        for (UserStatus status : UserStatus.values()) {
            User user = new User();
            user.setUsername("user");
            user.setPassword("encoded-password");
            user.setRole(Role.USER);
            user.setStatus(status);

            CustomUserDetails details = new CustomUserDetails(user);

            assertEquals(status == UserStatus.ACTIVE, details.isEnabled(), status.name());
        }
    }
}
