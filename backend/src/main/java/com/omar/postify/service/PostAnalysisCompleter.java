package com.omar.postify.service;

import com.omar.postify.dto.TopicCandidate;
import com.omar.postify.entities.Post;
import com.omar.postify.repository.PostTopicAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Handles the final step of post topic analysis: verifying ownership, replacing topics,
 * and marking the analysis record as COMPLETED — all within a single database transaction.
 *
 * This component exists solely to provide a separate Spring bean so that {@code @Transactional}
 * works correctly when called from {@link PostAnalysisService} (which avoids the Spring AOP
 * self-invocation problem).
 */
@Component
@RequiredArgsConstructor
public class PostAnalysisCompleter {

    private final PostTopicAnalysisRepository analysisQueue;
    private final TopicService topicService;

    /**
     * Atomically verify ownership, replace topics, and mark analysis as completed.
     *
     * All three operations run within a single database transaction so that a concurrent
     * enqueue (which bumps the revision) cannot slip between the ownership check and the
     * topic replacement. If ownership is lost, nothing is modified.
     *
     * @return true if ownership was verified and topics were replaced, false otherwise
     */
    @Transactional
    public boolean complete(Long postId, long revision, String claimToken,
                            Post post, List<TopicCandidate> candidates) {
        // Atomic ownership check: verify (postId, revision, claimToken, status=PROCESSING)
        // and mark COMPLETED in a single UPDATE statement.
        // The UPDATE acts as a row lock — if another transaction has already modified this row
        // (e.g., a new enqueue bumped the revision), this will return 0.
        int completed = analysisQueue.verifyAndComplete(
                postId,
                revision,
                claimToken,
                post.getContent(),
                Instant.now()
        );
        if (completed == 0) {
            return false;
        }

        // Ownership verified — replace topics within the same transaction.
        // If this throws, the entire transaction rolls back including the COMPLETED status,
        // allowing the job to be retried.
        topicService.replaceTopics(post, candidates);
        return true;
    }
}
