package com.omar.postify.service;

import com.omar.postify.entities.Post;
import com.omar.postify.entities.PostTopicAnalysis;
import com.omar.postify.entities.enums.PostStatus;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.PostTopicAnalysisRepository;
import com.omar.postify.service.ai.AIAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@lombok.RequiredArgsConstructor
@Slf4j
public class PostAnalysisService {

    private static final int BATCH_SIZE = 50;
    private static final Duration CLAIM_LEASE = Duration.ofMinutes(10);
    private static final long MAX_RETRY_DELAY_SECONDS = 3_600;
    private static final int MAX_ATTEMPTS = 5;

    private final PostTopicAnalysisRepository analysisQueue;
    private final PostRepository postRepository;
    private final AIAnalyzer aiAnalyzer;
    private final TopicService topicService;
    private final PostAnalysisCompleter analysisCompleter;

    @Transactional
    public void enqueuePostAnalysis(Long postId) {
        Instant now = Instant.now();
        analysisQueue.enqueue(postId, now, now);
    }

    @Scheduled(fixedDelayString = "${postify.topic-analysis.delay-ms:15000}")
    public void processPendingAnalysis() {
        Instant now = Instant.now();
        List<PostTopicAnalysis> due = analysisQueue
                .findByUpdatedAtLessThanEqualOrderByUpdatedAtAsc(
                        now,
                        PageRequest.of(0, BATCH_SIZE)
                );

        for (PostTopicAnalysis entry : due) {
            process(entry);
        }
    }

    private void process(PostTopicAnalysis entry) {
        Instant now = Instant.now();
        String claimToken = UUID.randomUUID().toString();
        Instant leaseUntil = now.plus(CLAIM_LEASE);
        int claimed = analysisQueue.claim(
                entry.getPostId(),
                entry.getRevision(),
                now,
                leaseUntil,
                claimToken
        );
        if (claimed == 0) {
            return;
        }

        int attempt = entry.getAttempts() + 1;
        try {
            Post post = postRepository.findByIdWithMedia(entry.getPostId())
                    .orElse(null);
            if (post == null || post.getStatus() != PostStatus.ACTIVE) {
                int skipped = analysisQueue.markSkipped(
                        entry.getPostId(),
                        entry.getRevision(),
                        claimToken,
                        "Post deleted or inactive"
                );
                if (skipped == 0) {
                    log.warn(
                            "Post topic analysis claim lost on skip; postId={}, revision={}",
                            entry.getPostId(),
                            entry.getRevision()
                    );
                } else {
                    log.info(
                            "Post topic analysis skipped; post inactive or deleted; postId={}",
                            entry.getPostId()
                    );
                }
                return;
            }

            // Run AI analysis outside any DB transaction
            List<com.omar.postify.dto.TopicCandidate> candidates =
                    aiAnalyzer.analyze(post.getContent(), post.getMedia());

            // Complete analysis atomically: verify ownership, replace topics, and mark COMPLETED
            // all within a single database transaction so a concurrent enqueue cannot slip between
            // the ownership check and the topic replacement.
            boolean completed = analysisCompleter.complete(
                    entry.getPostId(),
                    entry.getRevision(),
                    claimToken,
                    post,
                    candidates
            );
            if (!completed) {
                log.warn(
                        "Post topic analysis stale worker detected; skipping topic replacement; postId={}, revision={}",
                        entry.getPostId(),
                        entry.getRevision()
                );
            }

        } catch (RuntimeException failure) {
            // Check if we've exceeded max attempts
            if (attempt >= MAX_ATTEMPTS) {
                String errorMsg = failure.getClass().getSimpleName() + ": " + failure.getMessage();
                if (errorMsg.length() > 1000) {
                    errorMsg = errorMsg.substring(0, 1000);
                }
                int marked = analysisQueue.markFailed(
                        entry.getPostId(),
                        entry.getRevision(),
                        claimToken,
                        errorMsg
                );
                if (marked == 0) {
                    log.warn(
                            "Post topic analysis claim lost on final failure; postId={}, revision={}",
                            entry.getPostId(),
                            entry.getRevision()
                    );
                } else {
                    log.error(
                            "Post topic analysis permanently failed after {} attempts; postId={}, errorType={}",
                            attempt,
                            entry.getPostId(),
                            failure.getClass().getSimpleName()
                    );
                }
            } else {
                long retrySeconds = Math.min(
                        MAX_RETRY_DELAY_SECONDS,
                        15L * (1L << Math.min(attempt, 8))
                );
                int retried = analysisQueue.scheduleRetry(
                        entry.getPostId(),
                        entry.getRevision(),
                        claimToken,
                        Instant.now().plusSeconds(retrySeconds)
                );
                if (retried == 0) {
                    log.warn(
                            "Post topic analysis claim lost on retry; postId={}, revision={}",
                            entry.getPostId(),
                            entry.getRevision()
                    );
                } else {
                    log.warn(
                            "Post topic analysis will retry; postId={}, attempt={}, retryIn={}s, errorType={}",
                            entry.getPostId(),
                            attempt,
                            retrySeconds,
                            failure.getClass().getSimpleName()
                    );
                }
            }
        }
    }
}
