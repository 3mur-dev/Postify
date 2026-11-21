package com.omar.postify.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverviewMetrics {
    private long totalUsers;
    private long totalPosts;
    private long totalComments;
    private long totalActiveUsers;
    private EngagementMetrics engagement;
}
