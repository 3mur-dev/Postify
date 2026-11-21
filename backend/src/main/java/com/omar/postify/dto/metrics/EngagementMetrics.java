package com.omar.postify.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementMetrics {
    private long totalLikes;
    private long totalComments;
    private double averageLikesPerPost;
    private double averageCommentsPerPost;
    private double averageInteractionsPerPost;
}
