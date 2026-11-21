import type { AdminDashboardResponse } from "../api/types";

export const adminDashboardFixture: AdminDashboardResponse = {
  overview: {
    totalUsers: 12_345,
    activeUsers: 8_765,
    totalPosts: 4_321,
    totalComments: 9_876,
    totalLikes: 54_321,
  },
  users: {
    bannedUsers: 12,
    suspendedUsers: 34,
    newUsersInPeriod: 567,
    userGrowthPercentage: 12.34,
  },
  engagement: {
    likesReceived: 1_234,
    commentsReceived: 567,
    averageLikesPerPost: 8.25,
    averageCommentsPerPost: 2.5,
    engagementRate: 6.75,
  },
  system: {
    cpuUsage: 42.5,
    memoryUsage: 72.2,
    diskUsage: 91.5,
    uptimeSeconds: 183_845,
  },
  userGrowthChart: [{ label: "Mon", value: 12 }],
  postGrowthChart: [{ label: "Mon", value: 8 }],
  engagementChart: [{ label: "Mon", value: 20 }],
};
