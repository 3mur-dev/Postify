package com.omar.postify.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RankingFeatures(

        Long postId,
        Instant postCreatedAt,

        double userActivityLevel,
        double userSessionLengthMean,
        double userExplorationRate,
        double userEngagementBias,

        long historyImpressions,
        long historyViews,
        long historyLikes,
        long historySaves,
        long historyComments,
        long historyShares,

        double historyLikeRate,
        double historySaveRate,
        double historyCommentRate,
        double historyShareRate,
        double historyEngagementRate,

        double historyAvgDwellMs,
        double historyAvgCompletion,

        double durationMs,

        int topicCount,
        double topicRelevance,

        String contentType,
        String mediaType
) {}