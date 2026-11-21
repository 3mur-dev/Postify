package com.omar.postify.repository;

import com.omar.postify.entities.PostTopicAnalysis;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostTopicAnalysisRepository extends JpaRepository<PostTopicAnalysis, Long> {

    Optional<PostTopicAnalysis> findByPostId(Long postId);

    List<PostTopicAnalysis> findByStatus(String status);

    List<PostTopicAnalysis> findByStatusIn(List<String> statuses);

    List<PostTopicAnalysis> findByUpdatedAtLessThanEqualOrderByUpdatedAtAsc(
            Instant updatedAt, Pageable pageable
    );

    /**
     * Enqueue a post for topic analysis.
     * If an entry already exists for the post, bump its revision and reset to PENDING.
     * Returns 1 if a row was inserted or updated.
     */
    @Modifying
    @Transactional
    @Query(value = """
            insert into post_topic_analysis (post_id, status, updated_at, revision, attempts, created_at)
            values (:postId, 'PENDING', :updatedAt, 1, 0, :createdAt)
            on duplicate key update
                status = 'PENDING',
                updated_at = values(updated_at),
                revision = revision + 1,
                attempts = 0,
                claim_token = NULL,
                lease_expiry = NULL,
                error_message = NULL
            """, nativeQuery = true)
    int enqueue(
            @Param("postId") Long postId,
            @Param("updatedAt") Instant updatedAt,
            @Param("createdAt") Instant createdAt
    );

    /**
     * Atomically claim a job for processing.
     * Succeeds only if the entry matches the expected revision and is not already claimed (or claim is expired).
     * Returns 1 if claimed, 0 otherwise.
     */
    @Modifying
    @Transactional
    @Query("""
            UPDATE PostTopicAnalysis p
            SET p.claimToken = :claimToken, p.leaseExpiry = :leaseUntil, p.status = 'PROCESSING'
            WHERE p.postId = :postId
              AND p.revision = :revision
              AND (p.claimToken IS NULL OR p.leaseExpiry < :now)
              AND p.status IN ('PENDING', 'PROCESSING')
                          
            """)
    int claim(
            @Param("postId") Long postId,
            @Param("revision") long revision,
            @Param("now") Instant now,
            @Param("leaseUntil") Instant leaseUntil,
            @Param("claimToken") String claimToken
    );

    /**
     * Schedule a retry after failure.
     * Resets claim and sets a future updatedAt for retry eligibility.
     */
    @Modifying
    @Transactional
    @Query("""
            UPDATE PostTopicAnalysis p
            SET p.status = 'PENDING',
                p.updatedAt = :retryAt,
                p.attempts = p.attempts + 1,
                p.claimToken = NULL,
                p.leaseExpiry = NULL
            WHERE p.postId = :postId
              AND p.revision = :revision
              AND p.claimToken = :claimToken
            """)
    int scheduleRetry(
            @Param("postId") Long postId,
            @Param("revision") long revision,
            @Param("claimToken") String claimToken,
            @Param("retryAt") Instant retryAt
    );

    /**
     * Mark an entry as permanently failed.
     */
    @Modifying
    @Transactional
    @Query("""
            UPDATE PostTopicAnalysis p
            SET p.status = 'FAILED',
                p.errorMessage = :errorMessage,
                p.claimToken = NULL,
                p.leaseExpiry = NULL
            WHERE p.postId = :postId
              AND p.revision = :revision
              AND p.claimToken = :claimToken
            """)
    int markFailed(
            @Param("postId") Long postId,
            @Param("revision") long revision,
            @Param("claimToken") String claimToken,
            @Param("errorMessage") String errorMessage
    );

    /**
     * Mark an entry as successfully completed.
     */
    @Modifying
    @Transactional
    @Query("""
            UPDATE PostTopicAnalysis p
            SET p.status = 'COMPLETED',
                p.analyzedContent = :analyzedContent,
                p.analyzedAt = :analyzedAt,
                p.claimToken = NULL,
                p.leaseExpiry = NULL
            WHERE p.postId = :postId
              AND p.revision = :revision
              AND p.claimToken = :claimToken
            """)
    int markCompleted(
            @Param("postId") Long postId,
            @Param("revision") long revision,
            @Param("claimToken") String claimToken,
            @Param("analyzedContent") String analyzedContent,
            @Param("analyzedAt") Instant analyzedAt
    );

    /**
     * Atomically verify ownership and mark as completed.
     * Used inside a transactional service method that also replaces topics.
     * This method locks the row (via UPDATE) and checks (postId, revision, claimToken, status=PROCESSING).
     * Returns 1 if ownership was verified and entry was completed, 0 if ownership was lost.
     */
    @Modifying
    @Query("""
            UPDATE PostTopicAnalysis p
            SET p.status = 'COMPLETED',
                p.analyzedContent = :analyzedContent,
                p.analyzedAt = :analyzedAt,
                p.claimToken = NULL,
                p.leaseExpiry = NULL
            WHERE p.postId = :postId
              AND p.revision = :revision
              AND p.claimToken = :claimToken
              AND p.status = 'PROCESSING'
            """)
    int verifyAndComplete(
            @Param("postId") Long postId,
            @Param("revision") long revision,
            @Param("claimToken") String claimToken,
            @Param("analyzedContent") String analyzedContent,
            @Param("analyzedAt") Instant analyzedAt
    );

    /**
     * Mark an entry as skipped (e.g., post was deleted or inactive).
     * Skipped entries are terminal and will not be picked up for processing again.
     */
    @Modifying
    @Transactional
    @Query("""
            UPDATE PostTopicAnalysis p
            SET p.status = 'SKIPPED',
                p.errorMessage = :errorMessage,
                p.claimToken = NULL,
                p.leaseExpiry = NULL
            WHERE p.postId = :postId
              AND p.revision = :revision
              AND p.claimToken = :claimToken
            """)
    int markSkipped(
            @Param("postId") Long postId,
            @Param("revision") long revision,
            @Param("claimToken") String claimToken,
            @Param("errorMessage") String errorMessage
    );

    /**
     * Find expired claims that can be recovered.
     */
    List<PostTopicAnalysis> findByStatusAndLeaseExpiryBefore(String status, Instant before);
}
