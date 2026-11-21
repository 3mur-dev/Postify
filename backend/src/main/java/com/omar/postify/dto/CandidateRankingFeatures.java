package com.omar.postify.dto;

import java.time.Instant;

public record CandidateRankingFeatures(
        Long postId,
        Instant postCreatedAt,

        long durationMs,
        int topicCount,
        double topicRelevance,
        double postAgeHours,
        int sessionHour,
        int sessionDayOfWeek,
        String contentType,
        String mediaType
) {}
