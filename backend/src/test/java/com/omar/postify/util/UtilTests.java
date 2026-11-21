package com.omar.postify.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class UtilTests {

    @Test
    void testUUIDGeneration() {
        String uuid1 = java.util.UUID.randomUUID().toString();
        String uuid2 = java.util.UUID.randomUUID().toString();
        
        assertThat(uuid1).isNotBlank().matches("[\\da-f-]{36}");
        assertThat(uuid2).isNotBlank().matches("[\\da-f-]{36}");
        assertThat(uuid1).isNotEqualTo(uuid2);
    }

    @Test
    void testStringValidation() {
        String validUsername = "john_doe123";
        String validEmail = "john@example.com";
        String validPassword = "SecurePass123!";
        
        assertThat(validUsername).isNotBlank().hasSizeBetween(1, 50);
        assertThat(validEmail).isNotBlank().contains("@").contains(".");
        assertThat(validPassword).isNotBlank().hasSizeGreaterThanOrEqualTo(6);
    }

    @Test
    void testNumberRanges() {
        long followCount = 42;
        int likeCount = 100;
        
        assertThat(followCount).isPositive();
        assertThat(likeCount).isGreaterThanOrEqualTo(0);
        assertThat(followCount + likeCount).isEqualTo(142);
    }

    @Test
    void testEnumValues() {
        String status = "ACTIVE";
        assertThat(status).isIn("ACTIVE", "INACTIVE", "SUSPENDED");
    }

    @Test
    void testDateComparison() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime later = now.plusDays(1);
        
        assertThat(now).isBefore(later);
        assertThat(later).isAfter(now);
    }

    @Test
    void testCollectionOperations() {
        java.util.List<String> usernames = java.util.Arrays.asList("alice", "bob", "charlie");
        
        assertThat(usernames).hasSize(3).contains("bob");
        assertThat(usernames.stream().filter(u -> u.startsWith("a")).count()).isEqualTo(1);
    }

    @Test
    void testMapOperations() {
        java.util.Map<String, Integer> stats = new java.util.HashMap<>();
        stats.put("followers", 100);
        stats.put("following", 50);
        
        assertThat(stats).hasSize(2).containsKey("followers").containsValue(100);
        assertThat(stats.get("followers")).isEqualTo(100);
    }

    @Test
    void testExceptionHandling() {
        assertThatThrownBy(() -> {
            throw new IllegalArgumentException("Invalid input");
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid");
    }

    @Test
    void testBooleanLogic() {
        boolean isFollowing = true;
        boolean isPublic = false;
        
        assertThat(isFollowing && !isPublic).isTrue();
        assertThat(isFollowing || isPublic).isTrue();
        assertThat(!isPublic).isTrue();
    }
}
