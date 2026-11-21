package com.omar.postify.service;

import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.PostVectorSyncRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostVectorSyncServiceTest {

    @Mock private PostVectorSyncRepository syncRepository;
    @Mock private PostRepository postRepository;
    @Mock private com.omar.postify.service.ai.LocalAIEmbeddingService embeddingService;
    @Mock private com.omar.postify.service.ai.QdrantVectorStore vectorStore;

    @InjectMocks
    private PostVectorSyncService postVectorSyncService;

    @Test
    void schedulesSyncWhenCalled() {
        postVectorSyncService.scheduleSync(1L);

        verify(syncRepository).enqueue(anyLong(), any(Instant.class));
    }
}
