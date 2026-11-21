package com.omar.postify.service;

import com.omar.postify.dto.PostFeedDto;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.mapper.PostMapper;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.storage.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceCreateTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private com.omar.postify.repository.CommentRepository commentRepository;
    @Mock private com.omar.postify.repository.LikeRepository likeRepository;
    @Mock private com.omar.postify.repository.SavedPostRepository savedPostRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private PostMapper postMapper;
    @Mock private PostAnalysisService postAnalysisService;
    @Mock private PostVectorSyncService postVectorSyncService;

    @InjectMocks
    private PostService postService;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createsPostSuccessfully() {
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        User user = User.builder().id(1L).username("member").build();
        when(userRepository.findByUsername("member")).thenReturn(Optional.of(user));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(likeRepository.existsByUserAndPost(any(), any())).thenReturn(false);
        when(postMapper.toPostFeedDto(any(Post.class), any(Boolean.class), any(Boolean.class)))
                .thenReturn(mockPostFeedDto());

        PostFeedDto result = postService.createPost("test content", "member", List.of());

        assertNotNull(result);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void createsPostWithMedia() {
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        User user = User.builder().id(1L).username("member").build();
        when(userRepository.findByUsername("member")).thenReturn(Optional.of(user));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fileStorageService.storeMedia(any()))
                .thenReturn(new com.omar.postify.dto.StorageMediaResult(
                        "/images/posts/test.jpg",
                        com.omar.postify.entities.enums.MediaType.IMAGE,
                        null, null, 100L
                ));
        when(likeRepository.existsByUserAndPost(any(), any())).thenReturn(false);
        when(postMapper.toPostFeedDto(any(Post.class), any(Boolean.class), any(Boolean.class)))
                .thenReturn(mockPostFeedDto());

        MockMultipartFile media = new MockMultipartFile(
                "media", "test.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        PostFeedDto result = postService.createPost("test content", "member", List.of(media));

        assertNotNull(result);
        verify(postRepository).save(any(Post.class));
    }

    private PostFeedDto mockPostFeedDto() {
        return new PostFeedDto(
                1L,
                "pub-1",
                "test content",
                List.of(),
                java.time.LocalDateTime.now(),
                "member",
                null,
                0L,
                0L,
                false,
                false
        );
    }
}
