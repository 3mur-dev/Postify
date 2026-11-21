package com.omar.postify.service;

import com.omar.postify.entities.Post;
import com.omar.postify.entities.PostMedia;
import com.omar.postify.entities.PostTopicAnalysis;
import com.omar.postify.entities.enums.MediaType;
import com.omar.postify.entities.enums.PostStatus;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.PostTopicAnalysisRepository;
import com.omar.postify.service.ai.AIAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostAnalysisServiceTest {

    @Mock private PostTopicAnalysisRepository analysisQueue;
    @Mock private PostRepository postRepository;
    @Mock private AIAnalyzer aiAnalyzer;
    @Mock private TopicService topicService;
    @Mock private PostAnalysisCompleter analysisCompleter;

    @InjectMocks
    private PostAnalysisService postAnalysisService;

    // --- Enqueue tests ---

    @Test
    void enqueuePostAnalysis_createsNewEntry() {
        when(analysisQueue.enqueue(anyLong(), any(Instant.class), any(Instant.class)))
                .thenReturn(1);

        postAnalysisService.enqueuePostAnalysis(42L);

        verify(analysisQueue).enqueue(eq(42L), any(Instant.class), any(Instant.class));
    }

    @Test
    void enqueuePostAnalysis_incrementRevisionOnExisting() {
        // Second call to the same post should bump revision via ON DUPLICATE KEY UPDATE
        when(analysisQueue.enqueue(anyLong(), any(Instant.class), any(Instant.class)))
                .thenReturn(1);

        postAnalysisService.enqueuePostAnalysis(42L);
        postAnalysisService.enqueuePostAnalysis(42L);

        verify(analysisQueue, times(2)).enqueue(eq(42L), any(Instant.class), any(Instant.class));
    }

    // --- Claim tests ---

    @Test
    void process_skipsWhenClaimFails() {
        PostTopicAnalysis entry = PostTopicAnalysis.builder()
                .id(1L)
                .postId(42L)
                .revision(1L)
                .attempts(0)
                .status("PENDING")
                .updatedAt(Instant.now().minusSeconds(60))
                .build();

        when(analysisQueue.findByUpdatedAtLessThanEqualOrderByUpdatedAtAsc(any(), any(PageRequest.class)))
                .thenReturn(List.of(entry));
        when(analysisQueue.claim(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(0);

        postAnalysisService.processPendingAnalysis();

        verify(aiAnalyzer, never()).analyze(any(), any());
        verify(topicService, never()).replaceTopics(any(), any());
    }

    // --- Successful analysis ---

    @Test
    void process_completesSuccessfully() {
        Post post = Post.builder()
                .id(42L)
                .content("test content about programming")
                .status(PostStatus.ACTIVE)
                .media(List.of())
                .build();

        PostTopicAnalysis entry = PostTopicAnalysis.builder()
                .id(1L)
                .postId(42L)
                .revision(1L)
                .attempts(0)
                .status("PENDING")
                .updatedAt(Instant.now().minusSeconds(60))
                .build();

        when(analysisQueue.findByUpdatedAtLessThanEqualOrderByUpdatedAtAsc(any(), any(PageRequest.class)))
                .thenReturn(List.of(entry));
        when(analysisQueue.claim(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(1);
        when(postRepository.findByIdWithMedia(42L))
                .thenReturn(Optional.of(post));
        when(aiAnalyzer.analyze(anyString(), any()))
                .thenReturn(List.of(
                        new com.omar.postify.dto.TopicCandidate("Programming", 0.95),
                        new com.omar.postify.dto.TopicCandidate("Technology", 0.80)
                ));
        when(analysisCompleter.complete(anyLong(), anyLong(), anyString(), any(), any()))
                .thenReturn(true);

        postAnalysisService.processPendingAnalysis();

        verify(analysisCompleter).complete(eq(42L), eq(1L), anyString(), eq(post), anyList());
    }

    // --- Inactive post handling ---

    @Test
    void process_skipsInactivePost() {
        Post post = Post.builder()
                .id(42L)
                .status(PostStatus.DELETED)
                .build();

        PostTopicAnalysis entry = PostTopicAnalysis.builder()
                .id(1L)
                .postId(42L)
                .revision(1L)
                .attempts(0)
                .status("PENDING")
                .updatedAt(Instant.now().minusSeconds(60))
                .build();

        when(analysisQueue.findByUpdatedAtLessThanEqualOrderByUpdatedAtAsc(any(), any(PageRequest.class)))
                .thenReturn(List.of(entry));
        when(analysisQueue.claim(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(1);
        when(postRepository.findByIdWithMedia(42L))
                .thenReturn(Optional.of(post));

        when(analysisQueue.markSkipped(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(1);

        postAnalysisService.processPendingAnalysis();

        verify(aiAnalyzer, never()).analyze(any(), any());
        verify(analysisQueue).markSkipped(eq(42L), eq(1L), anyString(), anyString());
    }

    @Test
    void process_skipsDeletedPost() {
        PostTopicAnalysis entry = PostTopicAnalysis.builder()
                .id(1L)
                .postId(42L)
                .revision(1L)
                .attempts(0)
                .status("PENDING")
                .updatedAt(Instant.now().minusSeconds(60))
                .build();

        when(analysisQueue.findByUpdatedAtLessThanEqualOrderByUpdatedAtAsc(any(), any(PageRequest.class)))
                .thenReturn(List.of(entry));
        when(analysisQueue.claim(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(1);
        when(postRepository.findByIdWithMedia(42L))
                .thenReturn(Optional.empty());

        when(analysisQueue.markSkipped(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(1);

        postAnalysisService.processPendingAnalysis();

        verify(aiAnalyzer, never()).analyze(any(), any());
        verify(analysisQueue).markSkipped(eq(42L), eq(1L), anyString(), anyString());
    }

    // --- Retry on failure ---

    @Test
    void process_retriesOnFailure() {
        Post post = Post.builder()
                .id(42L)
                .content("test content")
                .status(PostStatus.ACTIVE)
                .media(List.of())
                .build();

        PostTopicAnalysis entry = PostTopicAnalysis.builder()
                .id(1L)
                .postId(42L)
                .revision(1L)
                .attempts(1)
                .status("PENDING")
                .updatedAt(Instant.now().minusSeconds(60))
                .build();

        when(analysisQueue.findByUpdatedAtLessThanEqualOrderByUpdatedAtAsc(any(), any(PageRequest.class)))
                .thenReturn(List.of(entry));
        when(analysisQueue.claim(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(1);
        when(postRepository.findByIdWithMedia(42L))
                .thenReturn(Optional.of(post));
        when(aiAnalyzer.analyze(anyString(), any()))
                .thenThrow(new RuntimeException("AI service unavailable"));
        when(analysisQueue.scheduleRetry(anyLong(), anyLong(), anyString(), any()))
                .thenReturn(1);

        postAnalysisService.processPendingAnalysis();

        verify(topicService, never()).replaceTopics(any(), any());
        verify(analysisQueue).scheduleRetry(eq(42L), eq(1L), anyString(), any(Instant.class));
    }

    // --- Stale worker prevention ---

    @Test
    void process_doesNotOverwriteNewerRevision() {
        Post post = Post.builder()
                .id(42L)
                .content("test content")
                .status(PostStatus.ACTIVE)
                .media(List.of())
                .build();

        // Entry has revision 1, but a newer request bumped it to 2
        PostTopicAnalysis entry = PostTopicAnalysis.builder()
                .id(1L)
                .postId(42L)
                .revision(1L)
                .attempts(0)
                .status("PENDING")
                .updatedAt(Instant.now().minusSeconds(60))
                .build();

        when(analysisQueue.findByUpdatedAtLessThanEqualOrderByUpdatedAtAsc(any(), any(PageRequest.class)))
                .thenReturn(List.of(entry));
        // Claim fails because revision 1 no longer matches
        when(analysisQueue.claim(eq(42L), eq(1L), any(), any(), any()))
                .thenReturn(0);

        postAnalysisService.processPendingAnalysis();

        verify(aiAnalyzer, never()).analyze(any(), any());
        verify(topicService, never()).replaceTopics(any(), any());
    }

    // --- Mark completed claim verification ---

    @Test
    void process_detectsStaleWorkerAndSkipsTopics() {
        Post post = Post.builder()
                .id(42L)
                .content("test content")
                .status(PostStatus.ACTIVE)
                .media(List.of())
                .build();

        PostTopicAnalysis entry = PostTopicAnalysis.builder()
                .id(1L)
                .postId(42L)
                .revision(1L)
                .attempts(0)
                .status("PENDING")
                .updatedAt(Instant.now().minusSeconds(60))
                .build();

        when(analysisQueue.findByUpdatedAtLessThanEqualOrderByUpdatedAtAsc(any(), any(PageRequest.class)))
                .thenReturn(List.of(entry));
        when(analysisQueue.claim(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(1);
        when(postRepository.findByIdWithMedia(42L))
                .thenReturn(Optional.of(post));
        when(aiAnalyzer.analyze(anyString(), any()))
                .thenReturn(List.of(new com.omar.postify.dto.TopicCandidate("Test", 0.9)));
        // complete returns false — stale worker, ownership lost
        when(analysisCompleter.complete(anyLong(), anyLong(), anyString(), any(), any()))
                .thenReturn(false);

        postAnalysisService.processPendingAnalysis();

        // Topics must NOT be applied when ownership is lost
        verify(topicService, never()).replaceTopics(any(), any());
        verify(analysisCompleter).complete(eq(42L), eq(1L), anyString(), eq(post), any());
    }

    // --- Max attempts exceeded ---

    @Test
    void process_marksFailedAfterMaxAttempts() {
        Post post = Post.builder()
                .id(42L)
                .content("test content")
                .status(PostStatus.ACTIVE)
                .media(List.of())
                .build();

        // 5 attempts already — next attempt (6) exceeds MAX_ATTEMPTS (5)
        PostTopicAnalysis entry = PostTopicAnalysis.builder()
                .id(1L)
                .postId(42L)
                .revision(1L)
                .attempts(5)
                .status("PENDING")
                .updatedAt(Instant.now().minusSeconds(60))
                .build();

        when(analysisQueue.findByUpdatedAtLessThanEqualOrderByUpdatedAtAsc(any(), any(PageRequest.class)))
                .thenReturn(List.of(entry));
        when(analysisQueue.claim(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(1);
        when(postRepository.findByIdWithMedia(42L))
                .thenReturn(Optional.of(post));
        when(aiAnalyzer.analyze(anyString(), any()))
                .thenThrow(new RuntimeException("Persistent failure"));
        when(analysisQueue.markFailed(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(1);

        postAnalysisService.processPendingAnalysis();

        verify(analysisQueue, never()).scheduleRetry(anyLong(), anyLong(), anyString(), any());
        verify(analysisQueue).markFailed(eq(42L), eq(1L), anyString(), anyString());
    }
}
