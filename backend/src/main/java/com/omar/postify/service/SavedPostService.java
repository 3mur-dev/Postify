package com.omar.postify.service;

import com.omar.postify.dto.InteractionEventMessage;
import com.omar.postify.dto.RequestIdentity;
import com.omar.postify.dto.SavedPostResponse;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.SavedPost;
import com.omar.postify.entities.User;
import com.omar.postify.entities.enums.InteractionEventType;
import com.omar.postify.entities.enums.InteractionSource;
import com.omar.postify.entities.enums.PostStatus;
import com.omar.postify.exception.BadRequestException;
import com.omar.postify.exception.ForbiddenException;
import com.omar.postify.exception.ResourceNotFoundException;
import com.omar.postify.mapper.SavedPostMapper;
import com.omar.postify.repository.*;
import com.omar.postify.service.kafka.InteractionEventProducer;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class SavedPostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final SavedPostRepository savedPostRepository;
    private final SavedPostMapper savedPostMapper;
    private final LikeRepository likeRepository;
    private final InteractionEventProducer interactionEventProducer;


    @Transactional
    public void savePost(String publicId, Principal principal, InteractionSource source) {

             Post post = postRepository.findByPublicIdWithUserAndStatusIsActive(publicId)
                     .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (savedPostRepository.existsByUserIdAndPostPublicId(user.getId(), publicId)) {
            throw new BadRequestException("Post already saved");
        }

        SavedPost savedPost = SavedPost.builder()
                .user(user)
                .post(post)
                .build();

        savedPostRepository.save(savedPost);

        interactionEventProducer.publish(
                new InteractionEventMessage(
                        RequestIdentity.authenticated(user.getId()),
                        post.getId(),
                        InteractionEventType.SAVE,
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
    }

    @Transactional
    public void unsavePost(String publicId, Principal principal) {

        Post post = postRepository.findByPublicIdWithUserAndStatusIsActive(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SavedPost savedPost = savedPostRepository.findByUserIdAndPostPublicId(user.getId(), publicId)
                .orElseThrow(() -> new BadRequestException("Post not saved"));

        savedPostRepository.delete(savedPost);
    }

    @Transactional(readOnly = true)
    public Page<SavedPostResponse> getSavedPosts(
            User requestedOwner,
            User requester,
            Pageable pageable
    ) {
        if (!Objects.equals(requestedOwner.getId(), requester.getId())) {
            throw new ForbiddenException("Saved posts are private");
        }

        Page<SavedPost> savedPosts =
                savedPostRepository.findActivePostsByUser(
                        requestedOwner,
                        PostStatus.ACTIVE,
                        pageable
                );

        return savedPosts.map(savedPost -> {
            boolean likedByCurrentUser =
                    likeRepository.existsByUserAndPost(requester, savedPost.getPost());

            return savedPostMapper.toSavedPostResponse(
                    savedPost,
                    likedByCurrentUser
            );
        });
    }
}
