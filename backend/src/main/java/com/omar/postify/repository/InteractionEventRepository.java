package com.omar.postify.repository;

import com.omar.postify.dto.RequestIdentity;
import com.omar.postify.entities.InteractionEvent;
import com.omar.postify.entities.enums.InteractionEventType;
import com.omar.postify.service.feed.UserEngagementCounts;
import com.omar.postify.service.feed.UserInteractionHistoryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;

@Repository
public interface InteractionEventRepository extends JpaRepository<InteractionEvent, Long> {

    boolean existsByClientEventId(String clientEventId);

    @Modifying
    @Query("delete from InteractionEvent event where event.anonymousUser.id in :anonymousUserIds")
    int deleteByAnonymousUserIds(@Param("anonymousUserIds") Collection<Long> anonymousUserIds);

    @Query(value = """
    SELECT COALESCE(
        AVG(
            TIMESTAMPDIFF(
                SECOND,
                session_start,
                session_end
            )
        ),
        0
    )
    FROM (
        SELECT
            session_id,
            MIN(occurred_at) AS session_start,
            MAX(occurred_at) AS session_end
        FROM interaction_events
        WHERE user_id = :userId
          AND session_id IS NOT NULL
        GROUP BY session_id
    ) sessions
    """, nativeQuery = true)
    double getAverageSessionLength(
            @Param("userId") Long userId
    );
    @Query("""
    SELECT COUNT(i)
    FROM InteractionEvent i
    WHERE i.user.id = :userId
      AND i.occurredAt >= :since
""")
    long countRecentInteractions(
            @Param("userId") Long userId,
            @Param("since") Instant since
    );

    @Query("""
    SELECT COUNT(i)
    FROM InteractionEvent i
    JOIN PostTopic pt ON pt.post.id = i.post.id
    WHERE i.user.id = :userId
      AND i.occurredAt >= :since
""")
    long countTopicInteractions(
            @Param("userId") Long userId,
            @Param("since") Instant since
    );

    @Query("""
    SELECT COUNT(i)
    FROM InteractionEvent i
    WHERE i.user.id = :userId
      AND i.occurredAt >= :since
      AND NOT EXISTS (
          SELECT 1
          FROM PostTopic pt
          WHERE pt.post.id = i.post.id
            AND pt.topic.id IN :topTopicIds
      )
""")
    long countInteractionsOutsideTopTopics(
            @Param("userId") Long userId,
            @Param("since") Instant since,
            @Param("topTopicIds") Collection<Long> topTopicIds
    );

    @Query("""
    SELECT
        COALESCE(SUM(
            CASE
                WHEN i.eventType IN (
                    com.omar.postify.entities.enums.InteractionEventType.VIEW,
                    com.omar.postify.entities.enums.InteractionEventType.VIEW_START,
                    com.omar.postify.entities.enums.InteractionEventType.VIEW_END,
                    com.omar.postify.entities.enums.InteractionEventType.MEDIA_START,
                    com.omar.postify.entities.enums.InteractionEventType.MEDIA_PROGRESS,
                    com.omar.postify.entities.enums.InteractionEventType.MEDIA_COMPLETE
                )
                THEN 1
                ELSE 0
            END
        ), 0) AS passiveCount,

        COALESCE(SUM(
            CASE
                WHEN i.eventType IN (
                    com.omar.postify.entities.enums.InteractionEventType.LIKE,
                    com.omar.postify.entities.enums.InteractionEventType.SAVE,
                    com.omar.postify.entities.enums.InteractionEventType.COMMENT,
                    com.omar.postify.entities.enums.InteractionEventType.SHARE
                )
                THEN 1
                ELSE 0
            END
        ), 0) AS activeCount

    FROM InteractionEvent i

    WHERE i.user.id = :userId
      AND i.occurredAt >= :since
""")
    UserEngagementCounts getEngagementCounts(
            @Param("userId") Long userId,
            @Param("since") Instant since
    );

    @Query("""
    SELECT COUNT(e)
    FROM InteractionEvent e
    WHERE e.user.id = :userId
      AND e.occurredAt >= :since
      AND e.eventType = :eventType
    """)
    long countByEventTypeSince(
            @Param("userId") Long userId,
            @Param("since") Instant since,
            @Param("eventType") InteractionEventType eventType
    );

    @Query("""
    SELECT
        COALESCE(SUM(
            CASE WHEN e.eventType =
                com.omar.postify.entities.enums.InteractionEventType.IMPRESSION
            THEN 1 ELSE 0 END
        ), 0) AS impressions,

        COALESCE(SUM(
            CASE WHEN e.eventType IN (
                com.omar.postify.entities.enums.InteractionEventType.VIEW,
                com.omar.postify.entities.enums.InteractionEventType.VIEW_START,
                com.omar.postify.entities.enums.InteractionEventType.VIEW_END
            )
            THEN 1 ELSE 0 END
        ), 0) AS views,

        COALESCE(SUM(
            CASE WHEN e.eventType =
                com.omar.postify.entities.enums.InteractionEventType.LIKE
            THEN 1 ELSE 0 END
        ), 0) AS likes,

        COALESCE(SUM(
            CASE WHEN e.eventType =
                com.omar.postify.entities.enums.InteractionEventType.SAVE
            THEN 1 ELSE 0 END
        ), 0) AS saves,

        COALESCE(SUM(
            CASE WHEN e.eventType =
                com.omar.postify.entities.enums.InteractionEventType.COMMENT
            THEN 1 ELSE 0 END
        ), 0) AS comments,

        COALESCE(SUM(
            CASE WHEN e.eventType =
                com.omar.postify.entities.enums.InteractionEventType.SHARE
            THEN 1 ELSE 0 END
        ), 0) AS shares,

        COALESCE(SUM(
            CASE
                WHEN e.durationMs IS NOT NULL
                THEN e.durationMs
                ELSE 0
            END
        ), 0) AS totalViewDurationMs,

        COALESCE(SUM(
            CASE
                WHEN e.durationMs IS NOT NULL
                THEN 1
                ELSE 0
            END
        ), 0) AS durationSamples,

        COALESCE(SUM(
            CASE
                WHEN e.completionRatio IS NOT NULL
                THEN e.completionRatio
                ELSE 0.0
            END
        ), 0.0) AS totalCompletionRatio,

        COALESCE(SUM(
            CASE
                WHEN e.completionRatio IS NOT NULL
                THEN 1
                ELSE 0
            END
        ), 0) AS completionRatioSamples

    FROM InteractionEvent e

    WHERE e.user.id = :userId
      AND e.occurredAt >= :since
""")
    UserInteractionHistoryProjection getUserInteractionHistory(
            @Param("userId") Long userId,
            @Param("since") Instant since
    );

    // =========================================================
    // ANONYMOUS
    // =========================================================

    @Query(value = """
        SELECT COALESCE(
            AVG(
                TIMESTAMPDIFF(
                    SECOND,
                    session_start,
                    session_end
                )
            ),
            0
        )
        FROM (
            SELECT
                session_id,
                MIN(occurred_at) AS session_start,
                MAX(occurred_at) AS session_end
            FROM interaction_events
            WHERE anonymous_user_id = :anonymousUserId
              AND session_id IS NOT NULL
            GROUP BY session_id
        ) sessions
        """, nativeQuery = true)
    double getAverageSessionLengthForAnonymous(
            @Param("anonymousUserId") Long anonymousUserId
    );

    @Query("""
        SELECT COUNT(i)
        FROM InteractionEvent i
        WHERE i.anonymousUser.id = :anonymousUserId
          AND i.occurredAt >= :since
        """)
    long countRecentInteractionsForAnonymous(
            @Param("anonymousUserId") Long anonymousUserId,
            @Param("since") Instant since
    );

    @Query("""
        SELECT COUNT(i)
        FROM InteractionEvent i
        JOIN PostTopic pt ON pt.post.id = i.post.id
        WHERE i.anonymousUser.id = :anonymousUserId
          AND i.occurredAt >= :since
        """)
    long countTopicInteractionsForAnonymous(
            @Param("anonymousUserId") Long anonymousUserId,
            @Param("since") Instant since
    );

    @Query("""
        SELECT COUNT(i)
        FROM InteractionEvent i
        WHERE i.anonymousUser.id = :anonymousUserId
          AND i.occurredAt >= :since
          AND NOT EXISTS (
              SELECT 1
              FROM PostTopic pt
              WHERE pt.post.id = i.post.id
                AND pt.topic.id IN :topTopicIds
          )
        """)
    long countInteractionsOutsideTopTopicsForAnonymous(
            @Param("anonymousUserId") Long anonymousUserId,
            @Param("since") Instant since,
            @Param("topTopicIds") Collection<Long> topTopicIds
    );

    @Query("""
        SELECT
            COALESCE(SUM(
                CASE
                    WHEN i.eventType IN (
                        com.omar.postify.entities.enums.InteractionEventType.VIEW,
                        com.omar.postify.entities.enums.InteractionEventType.VIEW_START,
                        com.omar.postify.entities.enums.InteractionEventType.VIEW_END,
                        com.omar.postify.entities.enums.InteractionEventType.MEDIA_START,
                        com.omar.postify.entities.enums.InteractionEventType.MEDIA_PROGRESS,
                        com.omar.postify.entities.enums.InteractionEventType.MEDIA_COMPLETE
                    )
                    THEN 1
                    ELSE 0
                END
            ), 0) AS passiveCount,

            COALESCE(SUM(
                CASE
                    WHEN i.eventType IN (
                        com.omar.postify.entities.enums.InteractionEventType.LIKE,
                        com.omar.postify.entities.enums.InteractionEventType.SAVE,
                        com.omar.postify.entities.enums.InteractionEventType.COMMENT,
                        com.omar.postify.entities.enums.InteractionEventType.SHARE
                    )
                    THEN 1
                    ELSE 0
                END
            ), 0) AS activeCount

        FROM InteractionEvent i

        WHERE i.anonymousUser.id = :anonymousUserId
          AND i.occurredAt >= :since
        """)
    UserEngagementCounts getEngagementCountsForAnonymous(
            @Param("anonymousUserId") Long anonymousUserId,
            @Param("since") Instant since
    );

    @Query("""
        SELECT COUNT(e)
        FROM InteractionEvent e
        WHERE e.anonymousUser.id = :anonymousUserId
          AND e.occurredAt >= :since
          AND e.eventType = :eventType
        """)
    long countByEventTypeSinceForAnonymous(
            @Param("anonymousUserId") Long anonymousUserId,
            @Param("since") Instant since,
            @Param("eventType") InteractionEventType eventType
    );

    @Query("""
        SELECT
            COALESCE(SUM(
                CASE WHEN e.eventType =
                    com.omar.postify.entities.enums.InteractionEventType.IMPRESSION
                THEN 1 ELSE 0 END
            ), 0) AS impressions,

            COALESCE(SUM(
                CASE WHEN e.eventType IN (
                    com.omar.postify.entities.enums.InteractionEventType.VIEW,
                    com.omar.postify.entities.enums.InteractionEventType.VIEW_START,
                    com.omar.postify.entities.enums.InteractionEventType.VIEW_END
                )
                THEN 1 ELSE 0 END
            ), 0) AS views,

            COALESCE(SUM(
                CASE WHEN e.eventType =
                    com.omar.postify.entities.enums.InteractionEventType.LIKE
                THEN 1 ELSE 0 END
            ), 0) AS likes,

            COALESCE(SUM(
                CASE WHEN e.eventType =
                    com.omar.postify.entities.enums.InteractionEventType.SAVE
                THEN 1 ELSE 0 END
            ), 0) AS saves,

            COALESCE(SUM(
                CASE WHEN e.eventType =
                    com.omar.postify.entities.enums.InteractionEventType.COMMENT
                THEN 1 ELSE 0 END
            ), 0) AS comments,

            COALESCE(SUM(
                CASE WHEN e.eventType =
                    com.omar.postify.entities.enums.InteractionEventType.SHARE
                THEN 1 ELSE 0 END
            ), 0) AS shares,

            COALESCE(SUM(
                CASE
                    WHEN e.durationMs IS NOT NULL
                    THEN e.durationMs
                    ELSE 0
                END
            ), 0) AS totalViewDurationMs,

            COALESCE(SUM(
                CASE
                    WHEN e.durationMs IS NOT NULL
                    THEN 1
                    ELSE 0
                END
            ), 0) AS durationSamples,

            COALESCE(SUM(
                CASE
                    WHEN e.completionRatio IS NOT NULL
                    THEN e.completionRatio
                    ELSE 0.0
                END
            ), 0.0) AS totalCompletionRatio,

            COALESCE(SUM(
                CASE
                    WHEN e.completionRatio IS NOT NULL
                    THEN 1
                    ELSE 0
                END
            ), 0) AS completionRatioSamples

        FROM InteractionEvent e

        WHERE e.anonymousUser.id = :anonymousUserId
          AND e.occurredAt >= :since
        """)
    UserInteractionHistoryProjection getUserInteractionHistoryForAnonymous(
            @Param("anonymousUserId") Long anonymousUserId,
            @Param("since") Instant since
    );

    @Query("""
    SELECT DISTINCT e.post.id
    FROM InteractionEvent e
    WHERE e.eventType = :eventType
      AND (
            (:userId IS NOT NULL AND e.user.id = :userId)
         OR (:anonymousUserId IS NOT NULL AND e.anonymousUser.id = :anonymousUserId)
      )
""")
    Set<Long> findSeenPostIds(
            @Param("userId") Long userId,
            @Param("anonymousUserId") Long anonymousUserId,
            @Param("eventType") InteractionEventType eventType
    );}
