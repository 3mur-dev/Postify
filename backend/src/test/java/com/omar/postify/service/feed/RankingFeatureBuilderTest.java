package com.omar.postify.service.feed;

import com.omar.postify.dto.*;
import com.omar.postify.entities.*;
import com.omar.postify.entities.enums.CandidateSource;
import com.omar.postify.entities.enums.InteractionEventType;
import com.omar.postify.entities.enums.Role;
import com.omar.postify.entities.enums.UserStatus;
import com.omar.postify.repository.InteractionEventRepository;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        properties = {
                "postify.ml.base-url=http://localhost:8000",
                "postify.ml.service-token=test-ml-token",
                "postify.cookie.secure=false"
        }
)
@Transactional
class RankingFeatureBuilderTest {

    private static final ZoneId KUWAIT =
            ZoneId.of("Asia/Kuwait");

    @Autowired
    private RankingFeatureBuilder rankingFeatureBuilder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private InteractionEventRepository interactionEventRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldBuildRankingFeatures() {

        // ============================================================
        // FIXED TEST TIME
        // ============================================================

        // Use dates within the last 30 days so they fall inside the production 30-day window
        LocalDateTime nowKuwait = LocalDateTime.now(KUWAIT);
        LocalDateTime sessionStartKuwait = nowKuwait.minusHours(2);

        Instant sessionStartedAt =
                sessionStartKuwait
                        .atZone(KUWAIT)
                        .toInstant();

        // ============================================================
        // USER
        // ============================================================

        User user =
                User.builder()
                        .publicId(UUID.randomUUID().toString())
                        .username("ranking-test-" + UUID.randomUUID())
                        .email(
                                "ranking-test-"
                                        + UUID.randomUUID()
                                        + "@example.com"
                        )
                        .password("test-password")
                        .role(Role.USER)
                        .status(UserStatus.ACTIVE)
                        .build();

        user = userRepository.save(user);

        entityManager.flush();

        final User testUser = user;

        // ============================================================
        // POSTS
        //
        // Post 1:
        // 13:00 Kuwait
        //
        // Session:
        // 15:00 Kuwait
        //
        // Age = 2 hours
        //
        // Post 2:
        // 10:00 Kuwait
        //
        // Age = 5 hours
        // ============================================================

        // Post 1: 2 hours before session (age = 2 hours from session)
        // Post 2: 5 hours before session (age = 5 hours from session)
        Post post1 =
                Post.builder()
                        .publicId(UUID.randomUUID().toString())
                        .user(testUser)
                        .content("Ranking test post 1")
                        .createdAt(sessionStartKuwait.minusHours(2))
                        .build();

        Post post2 =
                Post.builder()
                        .publicId(UUID.randomUUID().toString())
                        .user(testUser)
                        .content("Ranking test post 2")
                        .createdAt(sessionStartKuwait.minusHours(5))
                        .build();

        post1 = postRepository.save(post1);
        post2 = postRepository.save(post2);

        entityManager.flush();

        final Post finalPost1 = post1;
        final Post finalPost2 = post2;

        // ============================================================
        // INTERACTION DATA
        //
        // Expected:
        //
        // impressions = 10
        // views       = 6
        // likes       = 2
        // saves       = 2
        // comments    = 1
        // shares      = 1
        //
        // duration:
        // 2000 + 4000 = 6000
        //
        // completion:
        // 0.30 + 1.00 = 1.30
        //
        // average completion:
        // 1.30 / 2 = 0.65
        //
        // average dwell:
        // 6000 / 2 = 3000 ms
        // ============================================================

        List<InteractionEvent> events =
                List.of(
                        interaction(testUser, finalPost1, InteractionEventType.IMPRESSION, 60, null, null, sessionStartedAt),
                        interaction(testUser, finalPost1, InteractionEventType.IMPRESSION, 59, null, null, sessionStartedAt),
                        interaction(testUser, finalPost1, InteractionEventType.IMPRESSION, 58, null, null, sessionStartedAt),
                        interaction(testUser, finalPost1, InteractionEventType.IMPRESSION, 57, null, null, sessionStartedAt),
                        interaction(testUser, finalPost1, InteractionEventType.IMPRESSION, 56, null, null, sessionStartedAt),
                        interaction(testUser, finalPost2, InteractionEventType.IMPRESSION, 55, null, null, sessionStartedAt),
                        interaction(testUser, finalPost2, InteractionEventType.IMPRESSION, 54, null, null, sessionStartedAt),
                        interaction(testUser, finalPost2, InteractionEventType.IMPRESSION, 53, null, null, sessionStartedAt),
                        interaction(testUser, finalPost2, InteractionEventType.IMPRESSION, 52, null, null, sessionStartedAt),
                        interaction(testUser, finalPost2, InteractionEventType.IMPRESSION, 51, null, null, sessionStartedAt),
                        interaction(testUser, finalPost1, InteractionEventType.VIEW, 50, 2000L, 0.30, sessionStartedAt),
                        interaction(testUser, finalPost1, InteractionEventType.VIEW_START, 49, null, null, sessionStartedAt),
                        interaction(testUser, finalPost1, InteractionEventType.VIEW_END, 48, null, null, sessionStartedAt),
                        interaction(testUser, finalPost2, InteractionEventType.VIEW, 47, 4000L, 1.00, sessionStartedAt),
                        interaction(testUser, finalPost2, InteractionEventType.VIEW_START, 46, null, null, sessionStartedAt),
                        interaction(testUser, finalPost2, InteractionEventType.VIEW_END, 45, null, null, sessionStartedAt),
                        interaction(testUser, finalPost1, InteractionEventType.LIKE, 44, null, null, sessionStartedAt),
                        interaction(testUser, finalPost2, InteractionEventType.LIKE, 43, null, null, sessionStartedAt),
                        interaction(testUser, finalPost1, InteractionEventType.SAVE, 42, null, null, sessionStartedAt),
                        interaction(testUser, finalPost2, InteractionEventType.SAVE, 41, null, null, sessionStartedAt),
                        interaction(testUser, finalPost1, InteractionEventType.COMMENT, 40, null, null, sessionStartedAt),
                        interaction(testUser, finalPost1, InteractionEventType.SHARE, 39, null, null, sessionStartedAt)
                );

        interactionEventRepository.saveAll(events);

        entityManager.flush();
        entityManager.clear();

        // ============================================================
        // CANDIDATES
        // ============================================================

        List<FeedCandidate> candidates =
                List.of(
                        new FeedCandidate(
                                finalPost1.getId(),
                                CandidateSource.FOLLOWING,
                                0.85,
                                null
                        ),
                        new FeedCandidate(
                                finalPost2.getId(),
                                CandidateSource.EXPLORATION,
                                0.72,
                                null
                        )
                );

        // ============================================================
        // EXCLUSIONS
        // ============================================================

        FeedExclusionContext exclusions =
                new FeedExclusionContext(
                        Set.of(),
                        Set.of()
                );

        // ============================================================
        // CONTEXT
        // ============================================================

        CandidateGenerationContext context =
                new CandidateGenerationContext(
                        RequestIdentity.authenticated(testUser.getId()),
                        20,
                        Set.of(),
                        exclusions,
                        sessionStartedAt
                );

        // ============================================================
        // BUILD
        // ============================================================

        RankingFeatureRequest result =
                rankingFeatureBuilder.build(
                        context,
                        candidates
                );

        // ============================================================
        // BASIC ASSERTIONS
        // ============================================================

        assertNotNull(result);

        assertNotNull(result.candidates());

        assertEquals(
                2,
                result.candidates().size()
        );

        // ============================================================
        // GET FEATURES
        // ============================================================

        RankingFeatures features1 =
                result.candidates()
                        .stream()
                        .filter(candidate ->
                                candidate.postId()
                                        .equals(finalPost1.getId())
                        )
                        .findFirst()
                        .orElseThrow()
                        .features();

        RankingFeatures features2 =
                result.candidates()
                        .stream()
                        .filter(candidate ->
                                candidate.postId()
                                        .equals(finalPost2.getId())
                        )
                        .findFirst()
                        .orElseThrow()
                        .features();

        // ============================================================
        // USER FEATURES
        //
        // 22 interactions in the last 30 days.
        //
        // activityLevel =
        // 22 / (22 + 100)
        // = 0.180327...
        //
        // No session information in the events means the average
        // session length may be zero depending on the repository data.
        // ============================================================

        assertTrue(
                features1.userActivityLevel() > 0.0
        );

        assertTrue(
                features1.userActivityLevel() < 1.0
        );

        // ============================================================
        // HISTORY COUNTS
        // ============================================================

        assertEquals(
                10,
                features1.historyImpressions()
        );

        assertEquals(
                6,
                features1.historyViews()
        );

        assertEquals(
                2,
                features1.historyLikes()
        );

        assertEquals(
                2,
                features1.historySaves()
        );

        assertEquals(
                1,
                features1.historyComments()
        );

        assertEquals(
                1,
                features1.historyShares()
        );

        // ============================================================
        // HISTORY RATES
        // ============================================================

        assertEquals(
                0.20,
                features1.historyLikeRate(),
                0.000001
        );

        assertEquals(
                0.20,
                features1.historySaveRate(),
                0.000001
        );

        assertEquals(
                0.10,
                features1.historyCommentRate(),
                0.000001
        );

        assertEquals(
                0.10,
                features1.historyShareRate(),
                0.000001
        );

        // total engagements:
        //
        // 2 + 2 + 1 + 1 = 6
        //
        // 6 / 10 = 0.60

        assertEquals(
                0.60,
                features1.historyEngagementRate(),
                0.000001
        );

        // ============================================================
        // DWELL / COMPLETION
        // ============================================================

        assertEquals(
                3000.0,
                features1.historyAvgDwellMs(),
                0.000001
        );

        assertEquals(
                0.65,
                features1.historyAvgCompletion(),
                0.000001
        );

        // ============================================================
        // POST 1 FEATURES
        // ============================================================

        assertEquals(
                0.85,
                features1.topicRelevance(),
                0.000001
        );

        // Use a small delta for Instant comparison to handle nanosecond truncation
        Instant expectedPost1CreatedAt = sessionStartKuwait.minusHours(2)
                .atZone(KUWAIT)
                .toInstant();
        assertTrue(
                java.time.Duration.between(expectedPost1CreatedAt, features1.postCreatedAt()).toNanos() < 1_000_000,
                "Post 1 createdAt should match expected value within 1ms"
        );

        assertEquals(
                "TEXT",
                features1.contentType()
        );

        assertEquals(
                "NONE",
                features1.mediaType()
        );

        // ============================================================
        // POST 2 FEATURES
        // ============================================================

        assertEquals(
                0.72,
                features2.topicRelevance(),
                0.000001
        );

        // Use a small delta for Instant comparison to handle nanosecond truncation
        Instant expectedPost2CreatedAt = sessionStartKuwait.minusHours(5)
                .atZone(KUWAIT)
                .toInstant();
        assertTrue(
                java.time.Duration.between(expectedPost2CreatedAt, features2.postCreatedAt()).toNanos() < 1_000_000,
                "Post 2 createdAt should match expected value within 1ms"
        );

        assertEquals(
                "TEXT",
                features2.contentType()
        );

        assertEquals(
                "NONE",
                features2.mediaType()
        );

        // ============================================================
        // PRINT
        // ============================================================

        System.out.println();
        System.out.println(
                "=========================================="
        );
        System.out.println(
                "RANKING FEATURE BUILDER TEST"
        );
        System.out.println(
                "=========================================="
        );

        System.out.println(
                "USER ID: "
                        + testUser.getId()
        );

        System.out.println(
                "POST 1 ID: "
                        + finalPost1.getId()
        );

        System.out.println(
                "POST 2 ID: "
                        + finalPost2.getId()
        );

        System.out.println(
                "Input candidates: "
                        + candidates.size()
        );

        System.out.println(
                "Output candidates: "
                        + result.candidates().size()
        );

        System.out.println();

        result.candidates()
                .forEach(candidate -> {

                    System.out.println(
                            "------------------------------------------"
                    );

                    System.out.println(
                            "POST ID: "
                                    + candidate.postId()
                    );

                    System.out.println(
                            "FEATURES:"
                    );

                    System.out.println(
                            candidate.features()
                    );
                });

        System.out.println(
                "=========================================="
        );
    }

    private InteractionEvent interaction(
            User user,
            Post post,
            InteractionEventType type,
            long secondsAgo,
            Long durationMs,
            Double completionRatio,
            Instant sessionStartedAt
    ) {

        Instant occurredAt =
                sessionStartedAt
                        .minusSeconds(secondsAgo);

        return InteractionEvent.builder()
                .user(user)
                .post(post)
                .eventType(type)
                .occurredAt(occurredAt)
                .receivedAt(occurredAt)
                .sessionId("ranking-test-session")
                .clientEventId(UUID.randomUUID().toString())
                .durationMs(durationMs)
                .completionRatio(completionRatio)
                .eventVersion(1)
                .build();
    }
}