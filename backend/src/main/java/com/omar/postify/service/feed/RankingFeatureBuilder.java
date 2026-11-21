package com.omar.postify.service.feed;

import com.omar.postify.dto.*;
import com.omar.postify.entities.AnonymousUserTopicFeature;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.enums.MediaType;
import com.omar.postify.repository.AnonymousUserTopicFeatureRepository;
import com.omar.postify.repository.InteractionEventRepository;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserInterestRepository;
import com.omar.postify.util.UserTopicInterestCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingFeatureBuilder {

    private static final ZoneId KUWAIT_ZONE =
            ZoneId.of("Asia/Kuwait");

    private final InteractionEventRepository interactionEventRepository;
    private final UserInterestRepository userInterestRepository;
    private final PostRepository postRepository;
    private final AnonymousUserTopicFeatureRepository anonymousUserTopicFeatureRepository;
    private final UserTopicInterestCalculator interestCalculator;


    // ============================================================
    // PUBLIC ENTRY POINT
    // ============================================================

    public RankingFeatureRequest build(
            CandidateGenerationContext context,
            List<FeedCandidate> candidates
    ) {

        if (candidates == null || candidates.isEmpty()) {
            return new RankingFeatureRequest(List.of());
        }

        List<RankingFeatures> features =
                buildFeatures(
                        context,
                        candidates
                );

        List<RankingCandidate> rankingCandidates =
                new java.util.ArrayList<>();

        for (int i = 0; i < candidates.size(); i++) {

            FeedCandidate candidate =
                    candidates.get(i);

            RankingFeatures feature =
                    features.get(i);

            rankingCandidates.add(
                    new RankingCandidate(
                            candidate.postId(),
                            feature
                    )
            );
        }

        return new RankingFeatureRequest(
                rankingCandidates
        );
    }


    // ============================================================
    // BUILD FEATURES FOR ALL CANDIDATES
    // ============================================================

    public List<RankingFeatures> buildFeatures(CandidateGenerationContext context, List<FeedCandidate> candidates) {

        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }



        Instant now = Instant.now();
        Instant since = now.minus(30, ChronoUnit.DAYS);

        UserRankingFeatures userFeatures = buildUserFeatures(context, since);
        UserHistoryFeatures historyFeatures = buildUserHistoryFeatures(context, since);

        Set<Long> postIds = candidates.stream().map(FeedCandidate::postId).collect(Collectors.toSet());

        Map<Long, Post> postsById = postRepository.findAllById(postIds).stream()
                .collect(Collectors.toMap(
                        Post::getId,
                        Function.identity()
                ));

        return candidates.stream()
                .map(candidate -> {
                    Post post = postsById.get(candidate.postId()
                    );

                    if (post == null) {
                        throw new IllegalStateException("Post not found: " + candidate.postId());
                    }

                    CandidateRankingFeatures candidateFeatures = buildCandidateFeatures(candidate, post, now);

                    return flattenFeatures(userFeatures, historyFeatures, candidateFeatures);
                }).toList();

    }

    // ============================================================
    // USER FEATURES
    // ============================================================
    private UserRankingFeatures buildUserFeatures(
            CandidateGenerationContext context,
            Instant since
    ) {
        RequestIdentity identity = context.identity();

        long recentInteractions;
        double sessionLengthMean;
        long totalTopicInteractions;
        long outsideTopTopics;
        UserEngagementCounts counts;

        if (identity.isAuthenticated()) {

            Long userId = identity.id();

            recentInteractions =
                    interactionEventRepository
                            .countRecentInteractions(
                                    userId,
                                    since
                            );

            sessionLengthMean =
                    interactionEventRepository
                            .getAverageSessionLength(
                                    userId
                            );

            List<Long> topTopicIds =
                    userInterestRepository
                            .findTopTopicIds(
                                    userId,
                                    PageRequest.of(0, 3)
                            );

            totalTopicInteractions =
                    interactionEventRepository
                            .countTopicInteractions(
                                    userId,
                                    since
                            );

            outsideTopTopics =
                    topTopicIds.isEmpty()
                            ? 0
                            : interactionEventRepository
                            .countInteractionsOutsideTopTopics(
                                    userId,
                                    since,
                                    topTopicIds
                            );

            counts =
                    interactionEventRepository
                            .getEngagementCounts(
                                    userId,
                                    since
                            );

        } else {

            Long anonymousUserId = identity.id();

            recentInteractions =
                    interactionEventRepository
                            .countRecentInteractionsForAnonymous(
                                    anonymousUserId,
                                    since
                            );

            sessionLengthMean =
                    interactionEventRepository
                            .getAverageSessionLengthForAnonymous(
                                    anonymousUserId
                            );

            List<Long> topTopicIds =
                    anonymousUserTopicFeatureRepository
                            .findByAnonymousUserId(anonymousUserId)
                            .stream()
                            .sorted(
                                    Comparator.comparingDouble(
                                            (AnonymousUserTopicFeature feature) ->
                                                    interestCalculator.calculate(
                                                            feature
                                                    )
                                    ).reversed()
                            )
                            .limit(3)
                            .map(feature ->
                                    feature.getTopic().getId()
                            )
                            .toList();

            totalTopicInteractions =
                    interactionEventRepository
                            .countTopicInteractionsForAnonymous(
                                    anonymousUserId,
                                    since
                            );

            outsideTopTopics =
                    topTopicIds.isEmpty()
                            ? 0
                            : interactionEventRepository
                            .countInteractionsOutsideTopTopicsForAnonymous(
                                    anonymousUserId,
                                    since,
                                    topTopicIds
                            );

            counts =
                    interactionEventRepository
                            .getEngagementCountsForAnonymous(
                                    anonymousUserId,
                                    since
                            );
        }

        double activityLevel =
                recentInteractions /
                        (recentInteractions + 100.0);

        double explorationRate =
                totalTopicInteractions == 0
                        ? 0.0
                        : (double) outsideTopTopics
                        / totalTopicInteractions;

        long active = counts.getActiveCount();
        long passive = counts.getPassiveCount();

        long totalBias = active + passive;

        double engagementBias =
                totalBias == 0
                        ? 0.0
                        : (double) active / totalBias;

        return new UserRankingFeatures(
                activityLevel,
                sessionLengthMean,
                explorationRate,
                engagementBias
        );
    }


    private UserHistoryFeatures buildUserHistoryFeatures(
            CandidateGenerationContext context,
            Instant since
    ) {
        UserInteractionHistoryProjection counts;

        if (context.identity().isAuthenticated()) {

            counts =
                    interactionEventRepository
                            .getUserInteractionHistory(
                                    context.identity().id(),
                                    since
                            );

        } else {

            counts =
                    interactionEventRepository
                            .getUserInteractionHistoryForAnonymous(
                                    context.identity().id(),
                                    since
                            );
        }

        long impressions = counts.getImpressions();
        long views = counts.getViews();
        long likes = counts.getLikes();
        long saves = counts.getSaves();
        long comments = counts.getComments();
        long shares = counts.getShares();

        double likeRate =
                impressions == 0
                        ? 0.0
                        : (double) likes / impressions;

        double saveRate =
                impressions == 0
                        ? 0.0
                        : (double) saves / impressions;

        double commentRate =
                impressions == 0
                        ? 0.0
                        : (double) comments / impressions;

        double shareRate =
                impressions == 0
                        ? 0.0
                        : (double) shares / impressions;

        long totalEngagements =
                likes + saves + comments + shares;

        double engagementRate =
                impressions == 0
                        ? 0.0
                        : (double) totalEngagements / impressions;

        long totalViewDurationMs =
                counts.getTotalViewDurationMs();

        long completionRatioSamples =
                counts.getCompletionRatioSamples();

        double totalCompletionRatio =
                counts.getTotalCompletionRatio();

        long durationSamples =
                counts.getDurationSamples();

        double avgDwellMs =
                durationSamples == 0
                        ? 0.0
                        : (double) totalViewDurationMs
                        / durationSamples;

        double avgCompletion =
                completionRatioSamples == 0
                        ? 0.0
                        : totalCompletionRatio
                        / completionRatioSamples;

        return new UserHistoryFeatures(
                impressions,
                views,
                likes,
                saves,
                comments,
                shares,
                likeRate,
                saveRate,
                commentRate,
                shareRate,
                engagementRate,
                (long) avgDwellMs,
                avgCompletion
        );
    }

    // ============================================================
    // CANDIDATE FEATURES
    // ============================================================

    private CandidateRankingFeatures buildCandidateFeatures(
            FeedCandidate candidate,
            Post post,
            Instant now
    ) {

        Long postId = post.getId();

        Instant postCreatedAt = post.getCreatedAt()
                .atZone(KUWAIT_ZONE)
                .toInstant();

        long durationMs =
                getDurationMs(
                        post
                );


        int topicCount =
                getTopicCount(
                        post
                );


        double topicRelevance =
                candidate.relevanceScore();


        /*
         * IMPORTANT:
         *
         * Post.createdAt is LocalDateTime.
         *
         * Convert it using the application's actual timezone
         * before calculating its age.
         *
         * We calculate age against NOW, not sessionStartedAt.
         *
         * This prevents:
         *
         * "Post creation time cannot be after session start time"
         *
         * when a post was created after the browsing session began.
         */
        double postAgeHours =
                calculatePostAgeHours(
                        post.getCreatedAt(),
                        now
                );


        /*
         * Session context features.
         *
         * These describe WHEN the user is browsing.
         *
         * We get these from the current time here because the
         * builder does not need to mutate the candidate object.
         */
        ZonedDateTime sessionTime =
                now.atZone(
                        KUWAIT_ZONE
                );


        int sessionHour =
                sessionTime.getHour();


        int sessionDayOfWeek =
                sessionTime
                        .getDayOfWeek()
                        .getValue() - 1;


        String contentType =
                getContentType(
                        post
                );


        String mediaType =
                getMediaType(
                        post
                );


        return new CandidateRankingFeatures(
                postId,
                postCreatedAt,
                durationMs,
                topicCount,
                topicRelevance,
                postAgeHours,
                sessionHour,
                sessionDayOfWeek,
                contentType,
                mediaType
        );
    }


    // ============================================================
    // POST AGE
    // ============================================================

    private double calculatePostAgeHours(
            java.time.LocalDateTime postCreatedAt,
            Instant now
    ) {

        if (postCreatedAt == null) {
            return 0.0;
        }

        Instant postCreatedInstant =
                postCreatedAt
                        .atZone(
                                KUWAIT_ZONE
                        )
                        .toInstant();


        double ageHours =
                Duration.between(
                        postCreatedInstant,
                        now
                ).toSeconds()
                        / 3600.0;


        /*
         * Clock differences or unusual test data should never
         * produce a negative age.
         */
        return Math.max(
                0.0,
                ageHours
        );
    }


    // ============================================================
    // CONTENT TYPE
    // ============================================================

    private String getContentType(
            Post post
    ) {

        boolean hasImage =
                post.getMedia()
                        .stream()
                        .anyMatch(
                                media ->
                                        media.getType()
                                                == MediaType.IMAGE
                        );


        boolean hasVideo =
                post.getMedia()
                        .stream()
                        .anyMatch(
                                media ->
                                        media.getType()
                                                == MediaType.VIDEO
                        );


        if (hasImage && hasVideo) {
            return "MIXED";
        }

        if (hasVideo) {
            return "VIDEO";
        }

        if (hasImage) {
            return "IMAGE";
        }

        return "TEXT";
    }


    // ============================================================
    // MEDIA TYPE
    // ============================================================

    private String getMediaType(
            Post post
    ) {

        return post.getMedia()
                .stream()
                .findFirst()
                .map(media ->
                        media.getType().name()
                )
                .orElse("NONE");
    }


    // ============================================================
    // MEDIA DURATION
    // ============================================================

    private long getDurationMs(
            Post post
    ) {

        return post.getMedia()
                .stream()
                .mapToLong(
                        media ->
                                media.getDurationMs() == null
                                        ? 0L
                                        : media.getDurationMs()
                )
                .sum();
    }


    // ============================================================
    // TOPIC COUNT
    // ============================================================

    private int getTopicCount(
            Post post
    ) {

        return postRepository
                .countTopicsByPostId(
                        post.getId()
                );
    }


    // ============================================================
    // FLATTEN FEATURES
    // ============================================================

    private RankingFeatures flattenFeatures(
            UserRankingFeatures user,
            UserHistoryFeatures history,
            CandidateRankingFeatures candidate
    ) {

        return new RankingFeatures(

                // =================================================
                // CANDIDATE IDENTITY — 2
                // =================================================

                candidate.postId(),
                candidate.postCreatedAt(),

                // =================================================
                // USER FEATURES — 4
                // =================================================

                user.activityLevel(),
                user.sessionLengthMean(),
                user.explorationRate(),
                user.engagementBias(),


                // =================================================
                // HISTORY FEATURES — 13
                // =================================================

                history.impressions(),
                history.views(),
                history.likes(),
                history.saves(),
                history.comments(),
                history.shares(),

                history.likeRate(),
                history.saveRate(),
                history.commentRate(),
                history.shareRate(),
                history.engagementRate(),

                history.avgDwellMs(),
                history.avgCompletion(),


                // =================================================
                // CANDIDATE FEATURES — 8
                // =================================================

                candidate.durationMs(),
                candidate.topicCount(),
                candidate.topicRelevance(),

                candidate.contentType(),
                candidate.mediaType()
        );
    }
}