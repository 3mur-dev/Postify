package com.omar.postify.service;

import com.omar.postify.dto.LikeResponse;
import com.omar.postify.dto.InteractionEventMessage;
import com.omar.postify.dto.RequestIdentity;
import com.omar.postify.entities.*;
import com.omar.postify.entities.enums.InteractionEventType;
import com.omar.postify.entities.enums.InteractionSource;
import com.omar.postify.event.PostLikedEvent;
import com.omar.postify.exception.ResourceNotFoundException;
import com.omar.postify.repository.LikeRepository;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.kafka.InteractionEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final InteractionEventProducer interactionEventProducer;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LikeResponse toggleLike(String publicId, String username, InteractionSource source) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Post post = postRepository.findByPublicIdWithUserAndStatusIsActive(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        return likeRepository.findByUserIdAndPostPublicId(user.getId(), publicId)
                .map(existing -> {
                    likeRepository.delete(existing);

                    /* Use atomic DB-level decrement to avoid race conditions on concurrent unlikes. */
                    postRepository.decrementLikeCount(post.getId());
                    int newCount = postRepository.selectLikeCountById(post.getId());

                    interactionEventProducer.publish(
                            new InteractionEventMessage(
                                    RequestIdentity.authenticated(user.getId()),
                                    post.getId(),
                                    InteractionEventType.UNLIKE,
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

                    return new LikeResponse(false, newCount);

                })
                .orElseGet(() -> {
                    Like like = Like.builder()
                            .user(user)
                            .post(post)
                            .build();
                    likeRepository.save(like);

                    /* Use atomic DB-level increment to avoid race conditions on concurrent likes. */
                    postRepository.incrementLikeCount(post.getId());
                    int newCount = postRepository.selectLikeCountById(post.getId());

                    eventPublisher.publishEvent(
                            new PostLikedEvent(
                                    post.getId(),
                                    user.getId()
                            )
                    );
                    interactionEventProducer.publish(
                            new InteractionEventMessage(
                                    RequestIdentity.authenticated(user.getId()),
                                    post.getId(),
                                    InteractionEventType.LIKE,
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

                    return new LikeResponse(true, newCount);
                });

    }
}