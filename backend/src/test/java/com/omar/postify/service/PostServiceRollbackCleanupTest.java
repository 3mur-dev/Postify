package com.omar.postify.service;

import com.omar.postify.dto.StorageMediaResult;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.entities.enums.MediaType;
import com.omar.postify.mapper.PostMapper;
import com.omar.postify.repository.*;
import com.omar.postify.service.storage.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceRollbackCleanupTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private LikeRepository likeRepository;
    @Mock private SavedPostRepository savedPostRepository;
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
    void deletesUploadedMediaWhenPostTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();

        User user = new User();
        user.setId(1L);
        user.setUsername("member");
        when(userRepository.findByUsername("member")).thenReturn(Optional.of(user));
        when(fileStorageService.storeMedia(any()))
                .thenReturn(new StorageMediaResult(
                        "/videos/posts/uploaded.mp4",
                        MediaType.VIDEO,
                        null,
                        null,
                        1000L
                ));
        when(postRepository.save(any(Post.class)))
                .thenThrow(new IllegalStateException("db failure"));

        MockMultipartFile media = new MockMultipartFile(
                "media",
                "clip.mp4",
                "video/mp4",
                new byte[]{1}
        );

        assertThrows(
                IllegalStateException.class,
                () -> postService.createPost(null, "member", List.of(media))
        );

        TransactionSynchronizationUtils.triggerAfterCompletion(
                TransactionSynchronization.STATUS_ROLLED_BACK
        );

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(fileStorageService).deletePostMediaIfExists(postCaptor.capture());
        assertNotNull(postCaptor.getValue());
        assertEquals(1, postCaptor.getValue().getMedia().size());
        assertEquals(
                "/videos/posts/uploaded.mp4",
                postCaptor.getValue().getMedia().get(0).getUrl()
        );
    }
}
