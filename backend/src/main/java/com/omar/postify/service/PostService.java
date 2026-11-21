package com.omar.postify.service;

import com.omar.postify.dto.*;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.PostMedia;
import com.omar.postify.entities.User;
import com.omar.postify.exception.*;
import com.omar.postify.mapper.PostMapper;
import com.omar.postify.repository.*;
import com.omar.postify.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final CurrentUserService currentUserService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final SavedPostRepository savedPostRepository;
    private final FileStorageService fileStorageService;
    private final PostMapper postMapper;
    private final PostAnalysisService postAnalysisService;
    private final PostVectorSyncService postVectorSyncService;

    private static final int MAX_MEDIA_PER_POST = 10;

    public PostFeedDto getPostById(String publicId) {

        Post post = postRepository.findByPublicIdWithUserAndStatusIsActive(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with publicId: " + publicId));

        Optional<User> currentUser = currentUserService.getCurrentUser();

        boolean liked = currentUser.map(
                        user -> likeRepository.existsByUserAndPost(user, post))
                .orElse(false);
        boolean saved = currentUser.map(
                        user -> savedPostRepository.existsByUserAndPost(user, post))
                .orElse(false);

        return postMapper.toPostFeedDto(post, liked, saved);
    }

    @Transactional
    public PostFeedDto createPost(String content, String username, List<MultipartFile> media) {

        boolean hasContent = content != null && !content.trim().isEmpty();
        boolean hasMedia = media != null && !media.isEmpty();

        if (!hasContent && !hasMedia) {
            throw new BadRequestException("Post must contain text or media.");
        }


        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found: " + username));

        Post.PostBuilder builder = Post.builder()
                .content(hasContent ? content.trim() : null)
                .createdAt(LocalDateTime.now())
                .user(user)
                .publicId(UUID.randomUUID().toString());
        Post post = builder.build();
        registerMediaRollbackCleanup(post);


        if (hasMedia) {

            if (media.size() > MAX_MEDIA_PER_POST) {
                throw new BadRequestException(
                        "A post can contain at most " + MAX_MEDIA_PER_POST + " media files."
                );
            }

            for (int i = 0; i < media.size(); i++) {

                MultipartFile file = media.get(i);

                if (file == null || file.isEmpty()) {
                    continue;
                }

                StorageMediaResult result = fileStorageService.storeMedia(file);

                PostMedia postMedia = PostMedia.builder()
                        .post(post)
                        .url(result.url())
                        .type(result.type())
                        .position(i)
                        .width(result.width())
                        .height(result.height())
                        .durationMs(result.durationMs())
                        .build();

                post.getMedia().add(postMedia);
            }
        }

        Post saved = postRepository.save(post);
        postVectorSyncService.scheduleSync(saved.getId());
        postAnalysisService.enqueuePostAnalysis(saved.getId());

        boolean liked = likeRepository.existsByUserAndPost(user, saved);

        return postMapper.toPostFeedDto(saved, liked, false);
    }

    private void registerMediaRollbackCleanup(Post post) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            fileStorageService.deletePostMediaIfExists(post);
                        }
                    }
                }
        );
    }

    @Transactional
    public PostFeedDto updatePost(
            String publicId,
            String content,
            Principal principal
    ) {
        User currentUser = userRepository.findByUsername(principal.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Post post = postRepository.findByPublicIdWithUserAndStatusIsActive(publicId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Post not found with publicId: " + publicId
                        ));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException(
                    "You cannot update someone else's post"
            );
        }

        boolean hasContent =
                content != null && !content.trim().isEmpty();

        boolean hasMedia =
                post.getMedia() != null && !post.getMedia().isEmpty();

        if (!hasContent && !hasMedia) {
            throw new BadRequestException(
                    "Post must contain text or media."
            );
        }

        post.setContent(
                hasContent ? content.trim() : null
        );

        Post updatedPost = postRepository.save(post);

        boolean liked =
                likeRepository.existsByUserAndPost(
                        currentUser,
                        updatedPost
                );

        boolean saved =
                savedPostRepository.existsByUserAndPost(
                        currentUser,
                        updatedPost
                );

        postVectorSyncService.scheduleSync(updatedPost.getId());
        postAnalysisService.enqueuePostAnalysis(updatedPost.getId());

        return postMapper.toPostFeedDto(
                updatedPost,
                liked,
                saved
        );
    }

    @Transactional
    public void deletePost(String publicId, Principal principal) {

        User currentUser = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Post post = postRepository.findByPublicIdWithUserAndStatusIsActive(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException(
                    "You cannot delete someone else's post"
            );
        }

        postVectorSyncService.scheduleSync(post.getId());

        commentRepository.deleteByPostPublicId(publicId);
        likeRepository.deleteByPostPublicId(publicId);
        savedPostRepository.deleteByPostPublicId(publicId);
        postRepository.delete(post);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        fileStorageService.deletePostMediaIfExists(post);
                    }
                });
    }
}
