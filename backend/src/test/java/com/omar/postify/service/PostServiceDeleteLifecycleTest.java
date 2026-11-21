package com.omar.postify.service;

import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.repository.*;
import com.omar.postify.service.storage.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.security.Principal;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceDeleteLifecycleTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private LikeRepository likeRepository;
    @Mock private SavedPostRepository savedPostRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private com.omar.postify.mapper.PostMapper postMapper;
    @Mock private PostAnalysisService postAnalysisService;
    @Mock private TopicService topicService;
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
    void deleteRollbackPreservesMedia() {
        TransactionSynchronizationManager.initSynchronization();
        Post post = ownedPost();
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(post.getUser()));
        when(postRepository.findByPublicIdWithUserAndStatusIsActive("post"))
                .thenReturn(Optional.of(post));

        postService.deletePost("post", principal());
        TransactionSynchronizationUtils.triggerAfterCompletion(
                TransactionSynchronization.STATUS_ROLLED_BACK
        );

        verify(fileStorageService, never()).deletePostMediaIfExists(post);
    }

    @Test
    void deleteCommitRemovesMediaOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        Post post = ownedPost();
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(post.getUser()));
        when(postRepository.findByPublicIdWithUserAndStatusIsActive("post"))
                .thenReturn(Optional.of(post));

        postService.deletePost("post", principal());
        verify(fileStorageService, never()).deletePostMediaIfExists(post);

        TransactionSynchronizationUtils.triggerAfterCommit();

        verify(fileStorageService).deletePostMediaIfExists(post);
    }

    private Post ownedPost() {
        User user = new User();
        user.setId(9L);
        user.setUsername("owner");
        Post post = new Post();
        post.setId(12L);
        post.setPublicId("post");
        post.setUser(user);
        return post;
    }

    private Principal principal() {
        return () -> "owner";
    }
}
