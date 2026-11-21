package com.omar.postify.dto.metrics;

public record UserMetrics(

        long bannedUsers,
        long suspendedUsers,
        long newUsersInPeriod,
        double userGrowthPercentage
) {
}
