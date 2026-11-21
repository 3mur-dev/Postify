package com.omar.postify.service;

import com.omar.postify.entities.Post;
import com.omar.postify.entities.PostVectorSync;
import com.omar.postify.entities.enums.PostStatus;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.PostVectorSyncRepository;
import com.omar.postify.service.ai.LocalAIEmbeddingService;
import com.omar.postify.service.ai.QdrantVectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostVectorSyncService {

    private static final int BATCH_SIZE = 100;

    private final PostVectorSyncRepository syncRepository;
    private final PostRepository postRepository;
    private final LocalAIEmbeddingService embeddingService;
    private final QdrantVectorStore vectorStore;

    @Transactional
    public void scheduleSync(Long postId) {
        syncRepository.enqueue(postId, Instant.now());
    }

    @Scheduled(fixedDelayString = "${postify.vector-sync.delay-ms:15000}")
    public void synchronizePendingVectors() {
        List<PostVectorSync> pending = syncRepository.findAllByOrderByUpdatedAtAsc(
                PageRequest.of(0, BATCH_SIZE)
        );
        for (PostVectorSync entry : pending) {
            try {
                synchronize(entry);
            } catch (RuntimeException e) {
                syncRepository.markAttempted(
                        entry.getPostId(),
                        entry.getRevision(),
                        Instant.now()
                );
                log.error("Post vector sync failed for post {}", entry.getPostId(), e);
            }
        }
    }

    private void synchronize(PostVectorSync entry) {
        Post post = postRepository.findById(entry.getPostId()).orElse(null);
        if (post == null || post.getStatus() != PostStatus.ACTIVE
                || post.getContent() == null || post.getContent().isBlank()) {
            vectorStore.deletePostEmbedding(entry.getPostId());
        } else {
            vectorStore.savePostEmbedding(
                    post.getId(),
                    embeddingService.generateEmbedding(post.getContent())
            );
        }

        syncRepository.deleteIfUnchanged(entry.getPostId(), entry.getRevision());
    }
}
