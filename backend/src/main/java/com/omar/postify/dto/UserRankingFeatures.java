package com.omar.postify.dto;

public record UserRankingFeatures(
        double activityLevel,
        double sessionLengthMean,
        double explorationRate,
        double engagementBias
) {
}