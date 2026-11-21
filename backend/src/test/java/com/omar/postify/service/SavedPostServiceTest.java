package com.omar.postify.service;

import com.omar.postify.entities.User;
import com.omar.postify.entities.enums.PostStatus;
import com.omar.postify.exception.ForbiddenException;
import com.omar.postify.mapper.SavedPostMapper;
import com.omar.postify.repository.LikeRepository;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.SavedPostRepository;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.kafka.InteractionEventProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SavedPostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SavedPostRepository savedPostRepository;
    @Mock
    private SavedPostMapper savedPostMapper;
    @Mock
    private LikeRepository likeRepository;
    @Mock
    private InteractionEventProducer interactionEventProducer;

    @InjectMocks
    private SavedPostService savedPostService;

    @Test
    void rejectsReadingAnotherUsersSavedPostsBeforeQueryingRepository() {
        User owner = user(10L);
        User requester = user(20L);

        assertThrows(
                ForbiddenException.class,
                () -> savedPostService.getSavedPosts(
                        owner,
                        requester,
                        PageRequest.of(0, 20)
                )
        );

        verify(savedPostRepository, never())
                .findActivePostsByUser(owner, PostStatus.ACTIVE, PageRequest.of(0, 20));
    }

    @Test
    void queriesOnlyActiveSavedPostsForTheirOwner() {
        User owner = user(10L);
        PageRequest pageable = PageRequest.of(0, 20);
        org.mockito.Mockito.when(savedPostRepository.findActivePostsByUser(owner, PostStatus.ACTIVE, pageable))
                .thenReturn(org.springframework.data.domain.Page.empty(pageable));

        savedPostService.getSavedPosts(owner, owner, pageable);

        verify(savedPostRepository).findActivePostsByUser(owner, PostStatus.ACTIVE, pageable);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
