package com.omar.postify.service;

import com.omar.postify.entities.Post;
import com.omar.postify.entities.PostTopicAnalysis;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.PostTopicAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostTopicBackfillService {

    private final PostRepository postRepository;
    private final PostTopicAnalysisRepository analysisQueue;

    @Transactional
    public void backfill() {

        List<Post> posts = postRepository.findPostsWithoutTopics();

        log.info("Found {} posts without topics for backfill", posts.size());

        int enqueued = 0;
        int skipped = 0;

        for (Post post : posts) {
            try {
                // Skip posts without content — nothing to analyze
                if (post.getContent() == null || post.getContent().isBlank()
                        || (post.getMedia() != null && post.getMedia().isEmpty())) {
                    skipped++;
                    continue;
                }

                // Check if already queued
                analysisQueue.findByPostId(post.getId()).ifPresent(existing -> {
                    if ("PENDING".equals(existing.getStatus()) || "PROCESSING".equals(existing.getStatus())) {
                        // Already queued and not yet failed — skip
                        throw new SkippedException();
                    }
                });

                // Enqueue for async analysis
                analysisQueue.enqueue(post.getId(), java.time.Instant.now(), java.time.Instant.now());
                enqueued++;
                log.debug("Enqueued post {} for topic analysis backfill", post.getId());

            } catch (SkippedException e) {
                skipped++;
            } catch (Exception e) {
                log.error("Failed to enqueue post {} for topic analysis backfill", post.getId(), e);
            }
        }

        log.info(
                "Post topic backfill enqueue completed. enqueued={}, skipped={}",
                enqueued,
                skipped
        );
    }

    private static class SkippedException extends RuntimeException {
    }
}