package com.omar.postify.service;

import com.omar.postify.dto.CommentDto;
import com.omar.postify.dto.InteractionEventMessage;
import com.omar.postify.dto.RequestIdentity;
import com.omar.postify.entities.Comment;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.entities.enums.InteractionEventType;
import com.omar.postify.entities.enums.InteractionSource;
import com.omar.postify.exception.ResourceNotFoundException;
import com.omar.postify.event.CommentCreatedEvent;
import com.omar.postify.repository.CommentRepository;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.kafka.InteractionEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final InteractionEventProducer interactionEventProducer;

    @Transactional
    public CommentDto addComment(String publicId, String username, String content, InteractionSource source) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Post post = postRepository.findByPublicIdWithUserAndStatusIsActive(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment content cannot be empty");
        }

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(content.trim())
                .build();

        Comment saved = commentRepository.save(comment);

        /* Use atomic DB-level increment to avoid race conditions on concurrent comments. */
        postRepository.incrementCommentCount(post.getId());

        eventPublisher.publishEvent(
                new CommentCreatedEvent(
                        saved.getId(),
                        user.getId()
                )
        );

        interactionEventProducer.publish(
                new InteractionEventMessage(
                        RequestIdentity.authenticated(user.getId()),
                        post.getId(),
                        InteractionEventType.COMMENT,
                        Instant.now(),
                        null,
                        UUID.randomUUID().toString(),
                        null,
                        null,
                        null,
                        source,
                        null,
                        1
                )
        );

        return new CommentDto(
                saved.getId(),
                saved.getContent(),
                saved.getCreatedAt(),
                user.getPublicId(),
                user.getUsername(),
                user.getAvatarUrl(),
                post.getPublicId()
        );
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getComments(String publicId) {
        if (!postRepository.existsByPublicIdAndStatus(
                publicId,
                com.omar.postify.entities.enums.PostStatus.ACTIVE
        )) {
            throw new ResourceNotFoundException("Post not found");
        }
        return commentRepository.findDtosByPublicIdOrderByCreatedAtAsc(publicId);
    }
}
