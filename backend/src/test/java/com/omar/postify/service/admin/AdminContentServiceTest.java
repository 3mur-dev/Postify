package com.omar.postify.service.admin;

import com.omar.postify.dto.AdminContext;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.entities.enums.PostStatus;
import com.omar.postify.entities.enums.UserStatus;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.PostVectorSyncService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminContentServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostVectorSyncService postVectorSyncService;

    @InjectMocks
    private AdminContentService adminContentService;

    @Test
    void softDeletesPostWhenAdminRemovesContent() {
        String publicId = "post-123";
        AdminContext adminContext = new AdminContext(
                99L,
                "admin",
                "127.0.0.1",
                "JUnit"
        );

        User user = User.builder().id(1L).username("member").publicId("user-1").build();
        Post post = Post.builder()
                .id(1L)
                .publicId(publicId)
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();

        when(postRepository.findByPublicIdWithUserAndMedia(publicId))
                .thenReturn(Optional.of(post));

        when(postRepository.save(any(Post.class)))
                .thenReturn(post);

        adminContentService.softDeletePost(publicId, adminContext);

        assertEquals(PostStatus.DELETED, post.getStatus());
        assertNotNull(post.getDeletedAt());
        assertEquals(99L, post.getModeratedBy());

        verify(postRepository).save(post);
        verify(postVectorSyncService).scheduleSync(1L);
    }

    @Test
    void hidesPostWhenAdminModeratesContent() {
        String publicId = "post-123";
        AdminContext adminContext = new AdminContext(
                99L,
                "admin",
                "127.0.0.1",
                "JUnit"
        );

        User user = User.builder().id(1L).username("member").publicId("user-1").build();
        Post post = Post.builder()
                .id(1L)
                .publicId(publicId)
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();

        when(postRepository.findByPublicIdWithUserAndStatusIsActive(publicId))
                .thenReturn(Optional.of(post));

        when(postRepository.save(any(Post.class)))
                .thenReturn(post);

        adminContentService.hidePost(publicId, adminContext);

        assertEquals(PostStatus.HIDDEN, post.getStatus());
        assertEquals(99L, post.getModeratedBy());
        assertNull(post.getDeletedAt());

        verify(postRepository).save(post);
        verify(postVectorSyncService).scheduleSync(1L);
    }
}