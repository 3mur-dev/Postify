package com.omar.postify.service;

import com.omar.postify.dto.CommentDto;
import com.omar.postify.entities.enums.PostStatus;
import com.omar.postify.exception.ResourceNotFoundException;
import com.omar.postify.repository.CommentRepository;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.kafka.InteractionEventProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import com.omar.postify.service.CommentService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentVisibilityTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private InteractionEventProducer interactionEventProducer;

    @InjectMocks
    private CommentService commentService;

    @Test
    void hidesCommentsFromSuspendedUsers() {
        String publicId = "post-123";

        when(postRepository.existsByPublicIdAndStatus(
                publicId, PostStatus.ACTIVE
        )).thenReturn(true);

        when(commentRepository.findDtosByPublicIdOrderByCreatedAtAsc(publicId))
                .thenReturn(List.of());

        List<CommentDto> visibleComments =
                commentService.getComments(publicId);

        assertTrue(visibleComments.isEmpty());

        verify(commentRepository)
                .findDtosByPublicIdOrderByCreatedAtAsc(publicId);
    }

    @Test
    void showsCommentsFromActiveUsers() {
        String publicId = "post-123";

        CommentDto comment = mock(CommentDto.class);

        when(postRepository.existsByPublicIdAndStatus(
                publicId, PostStatus.ACTIVE
        )).thenReturn(true);

        when(commentRepository.findDtosByPublicIdOrderByCreatedAtAsc(publicId))
                .thenReturn(List.of(comment));

        List<CommentDto> visibleComments =
                commentService.getComments(publicId);

        assertEquals(1, visibleComments.size());
        assertSame(comment, visibleComments.get(0));
    }

    @Test
    void throwsWhenPostIsNotActive() {
        String publicId = "post-123";

        when(postRepository.existsByPublicIdAndStatus(
                publicId, PostStatus.ACTIVE
        )).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> commentService.getComments(publicId)
        );

        verifyNoInteractions(commentRepository);
    }
}