package com.omar.postify.service.admin;

import com.omar.postify.dto.admin.AdminDashboardRequest;
import com.omar.postify.dto.admin.AdminDashboardResponse;
import com.omar.postify.dto.enums.DashboardSection;
import com.omar.postify.dto.PeriodRange;
import com.omar.postify.dto.metrics.*;
import com.omar.postify.entities.enums.UserStatus;
import com.omar.postify.exception.BadRequestException;
import com.omar.postify.repository.CommentRepository;
import com.omar.postify.repository.LikeRepository;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Duration;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.io.File;
import java.util.*;


@Service
@AllArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;

    public AdminDashboardResponse getDashboard(AdminDashboardRequest request) {

        LocalDateTime fromDate;
        LocalDateTime toDate;

        ZoneId zone = ZoneId.of(request.timezone().toString());
        LocalDateTime now = LocalDateTime.now(zone);

        switch (request.rangePreset()) {

            case CUSTOM -> {

                if (request.fromDate() == null || request.toDate() == null) {
                    throw new BadRequestException("Custom range requires dates");
                }
                if (request.fromDate().isAfter(request.toDate())) {
                    throw new BadRequestException("fromDate must be before toDate");
                }

                fromDate = request.fromDate();
                toDate = request.toDate();
            }

            case TODAY -> {
                fromDate = now.toLocalDate().atStartOfDay();
                toDate = now;
            }

            case LAST_7_DAYS -> {
                fromDate = now.minusDays(7);
                toDate = now;
            }

            case LAST_30_DAYS -> {
                fromDate = now.minusDays(30);
                toDate = now;
            }

            case LAST_90_DAYS -> {
                fromDate = now.minusDays(90);
                toDate = now;
            }


            default -> throw new BadRequestException("Unknown preset");
        }

        Set<DashboardSection> sections =
                request.sections() != null
                        ? new HashSet<>(request.sections())
                        : EnumSet.allOf(DashboardSection.class);

        OverviewMetrics overview = null;
        if (sections.contains(DashboardSection.OVERVIEW)) {
            overview = getOverviewMetrics();
        }

        UserMetrics users = null;
        if (sections.contains(DashboardSection.USER_METRICS)) {
            users = getUserMetrics(fromDate, toDate);
        }

        EngagementMetrics engagements = null;
        if (sections.contains(DashboardSection.ENGAGEMENT_METRICS)) {
            engagements = getEngagementMetrics(fromDate, toDate);
        }

        SystemMetrics system = null;
        if (sections.contains(DashboardSection.SYSTEM_METRICS)) {
            system = getSystemMetrics();
        }

        List<ChartDataPoint> userGrowth = null;
        List<ChartDataPoint> postGrowth = null;
        List<ChartDataPoint> engagementChart = null;

        if (sections.contains(DashboardSection.CHARTS)) {
            userGrowth = buildUserGrowthChart(fromDate, toDate);
            postGrowth = buildPostGrowthChart(fromDate, toDate);
            engagementChart = buildEngagementChart(fromDate, toDate);
        }

        return new AdminDashboardResponse(
                overview,
                users,
                engagements,
                system,
                userGrowth,
                postGrowth,
                engagementChart
        );
    }

    public OverviewMetrics getOverviewMetrics() {

        long totalUsers = userRepository.count();
        long totalActiveUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long totalPosts = postRepository.count();
        long totalComments = commentRepository.count();


        /*
         * Use simple COUNT queries instead of getEngagementMetrics with unbounded
         * date range (LocalDateTime.MIN to MAX) to avoid scanning the entire
         * likes/comments table which would be extremely slow on large datasets.
         */
        long totalLikes = likeRepository.count();
        long totalCommentsAll = commentRepository.count();
        double averageLikesPerPost = totalPosts > 0
                ? (double) totalLikes / totalPosts : 0;
        double averageCommentsPerPost = totalPosts > 0
                ? (double) totalCommentsAll / totalPosts : 0;
        double averageInteractionsPerPost = totalPosts > 0
                ? (double) (totalLikes + totalCommentsAll) / totalPosts : 0;

        return new OverviewMetrics(
                totalUsers,
                totalPosts,
                totalComments,
                totalActiveUsers,
                new EngagementMetrics(
                        totalLikes,
                        totalCommentsAll,
                        averageLikesPerPost,
                        averageCommentsPerPost,
                        averageInteractionsPerPost
                )
        );
    }

    public UserMetrics getUserMetrics(LocalDateTime fromDate, LocalDateTime toDate) {

        long totalBannedUsers = userRepository.countByStatus(UserStatus.BANNED);

        long totalSuspendedUsers = userRepository.countByStatus(UserStatus.SUSPENDED);

        PeriodRange currentPeriod = new PeriodRange(fromDate, toDate);
        PeriodRange previousPeriod = getPreviousPeriod(currentPeriod);

        long newUsersInPeriod =
                userRepository.countByCreatedAtBetween(currentPeriod.start(), currentPeriod.end());

        long previousPeriodNewUsers =
                userRepository.countByCreatedAtBetween(previousPeriod.start(), previousPeriod.end());

        double userGrowthPercentage = calculateGrowthPercentage(newUsersInPeriod, previousPeriodNewUsers);

        return new UserMetrics(
                totalBannedUsers,
                totalSuspendedUsers,
                newUsersInPeriod,
                userGrowthPercentage
        );
    }

    public EngagementMetrics getEngagementMetrics(LocalDateTime fromDate, LocalDateTime toDate) {

        long totalLikes = likeRepository.countByCreatedAtBetween(fromDate, toDate);

        long totalComments = commentRepository.countByCreatedAtBetween(fromDate, toDate);

        long totalPosts =
                postRepository.countByCreatedAtBetween(fromDate, toDate);

        double averageLikesPerPost =
                totalPosts > 0 ? (double) totalLikes / totalPosts : 0;

        double averageCommentsPerPost =
                totalPosts > 0 ? (double) totalComments / totalPosts : 0;

        double averageInteractionsPerPost =
                totalPosts > 0
                        ? (double) (totalLikes + totalComments) / totalPosts
                        : 0;

        return new EngagementMetrics(
                totalLikes,
                totalComments,
                averageLikesPerPost,
                averageCommentsPerPost,
                averageInteractionsPerPost
        );
    }

    public SystemMetrics getSystemMetrics() {

        OperatingSystemMXBean osBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        double cpuUsage = Math.max(0, osBean.getCpuLoad() * 100);

        /*
         * Use JVM heap memory instead of OS-level memory.
         * getTotalMemorySize()/getFreeMemorySize() on OperatingSystemMXBean report
         * OS physical memory, not JVM heap usage.
         */
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        double memoryUsage = totalMemory > 0
                ? ((double) (totalMemory - freeMemory) / totalMemory) * 100
                : 0;

        File root = new File(System.getProperty("user.dir"));

        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getUsableSpace();

        double diskUsage =
                ((double) (totalSpace - freeSpace) / totalSpace) * 100;

        long uptimeSeconds =
                ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        return new SystemMetrics(
                cpuUsage,
                memoryUsage,
                diskUsage,
                uptimeSeconds
        );

    }

    public List<ChartDataPoint> buildUserGrowthChart(LocalDateTime fromDate, LocalDateTime toDate) {

        return userRepository.countUsersGroupedByDay(fromDate, toDate)
                .stream()
                .map(row -> new ChartDataPoint(
                        ((LocalDateTime) row[0]).toLocalDate().toString(),
                        (Long) row[1]
                ))
                .toList();
    }

    public List<ChartDataPoint> buildPostGrowthChart(LocalDateTime fromDate, LocalDateTime toDate) {

        return postRepository.countPostsGroupedByDay(fromDate, toDate)
                .stream()
                .map(row -> new ChartDataPoint(
                        row[0].toString(),   // safe conversion
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    public List<ChartDataPoint> buildEngagementChart(
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {

        List<Object[]> commentsRaw =
                commentRepository.countCommentsGroupedByDay(fromDate, toDate);

        List<Object[]> likesRaw =
                likeRepository.countLikesGroupedByDay(fromDate, toDate);

        Map<LocalDate, Long> totals = new HashMap<>();

        for (Object[] row : commentsRaw) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = ((Number) row[1]).longValue();

            totals.merge(date, count, Long::sum);
        }

        for (Object[] row : likesRaw) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = ((Number) row[1]).longValue();

            totals.merge(date, count, Long::sum);
        }

        return totals.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ChartDataPoint(
                        entry.getKey().toString(),
                        entry.getValue()
                ))
                .toList();
    }

    private PeriodRange getPreviousPeriod(PeriodRange currentPeriod) {
        Duration duration = Duration.between(currentPeriod.start(), currentPeriod.end());

        if (duration.isNegative() || duration.isZero()) {
            return currentPeriod;
        }

        LocalDateTime previousTo = currentPeriod.start();
        LocalDateTime previousFrom = previousTo.minus(duration);

        return new PeriodRange(previousFrom, previousTo);
    }

    private double calculateGrowthPercentage(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0.0 : 100.0;
        }

        return ((double) (current - previous) / previous) * 100.0;
    }
}
