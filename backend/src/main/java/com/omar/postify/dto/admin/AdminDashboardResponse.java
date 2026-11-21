package com.omar.postify.dto.admin;

import com.omar.postify.dto.metrics.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminDashboardResponse(

        OverviewMetrics overview,

        UserMetrics users,

        EngagementMetrics engagement,

        SystemMetrics system,

        List<ChartDataPoint> userGrowthChart,

        List<ChartDataPoint> postGrowthChart,

        List<ChartDataPoint> engagementChart

) {
}

