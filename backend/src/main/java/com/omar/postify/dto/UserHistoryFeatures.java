package com.omar.postify.dto;

public record UserHistoryFeatures(
        long impressions,
        long views,
        long likes,
        long saves,
        long comments,
        long shares,

        double likeRate,
        double saveRate,
        double commentRate,
        double shareRate,
        double engagementRate,

        long avgDwellMs,
        double avgCompletion
) {
}