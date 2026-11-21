package com.omar.postify.service;

import com.omar.postify.dto.*;
import com.omar.postify.exception.ResourceNotFoundException;
import com.omar.postify.entities.User;
import com.omar.postify.mapper.FollowMapper;
import com.omar.postify.mapper.PostMediaMapper;
import com.omar.postify.mapper.UserMapper;
import com.omar.postify.repository.FollowRepository;
import com.omar.postify.repository.PostMediaRepository;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.storage.FileStorageService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FollowRepository followRepository;
    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final FileStorageService fileStorageService;
    private final PostMediaRepository postMediaRepository;
    private final PostMediaMapper postMediaMapper;

    public User getProfileUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public boolean isOwnProfile(User currentUser, User profileUser) {
        return currentUser != null && currentUser.getId().equals(profileUser.getId());
    }

    public boolean isFollowing(User currentUser, User profileUser) {
        if (currentUser == null || isOwnProfile(currentUser, profileUser)) return false;
        return followRepository.existsByFollowerAndFollowing(currentUser, profileUser);
    }

    @Transactional(readOnly = true)
    public List<ProfilePostDto> getUserPosts(String username) {

        List<ProfilePostBaseDto> posts =
                postRepository.findProfilePostsByUsername(username);

        if (posts.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = posts.stream()
                .map(ProfilePostBaseDto::id)
                .toList();

        List<PostMediaProjection> media =
                postMediaRepository.findMediaByPostIds(postIds);

        Map<Long, List<PostMediaDto>> mediaByPost =
                media.stream()
                        .collect(Collectors.groupingBy(
                                PostMediaProjection::postId,
                                LinkedHashMap::new,
                                Collectors.mapping(
                                        postMediaMapper::toDto,
                                        Collectors.toList()
                                )
                        ));

        return posts.stream()
                .map(post -> new ProfilePostDto(
                        post.id(),
                        post.publicId(),
                        post.content(),
                        mediaByPost.getOrDefault(post.id(), List.of()),
                        post.createdAt(),
                        post.likeCount(),
                        post.commentCount(),
                        post.username()
                ))
                .toList();
    }
    public long getFollowersCount(User profileUser) {
        return followRepository.countByFollowing(profileUser);
    }

    public long getFollowingCount(User profileUser) {
        return followRepository.countByFollower(profileUser);
    }

    public ProfileResponse getProfileResponse(User profileUser, User currentUser) {
        boolean isOwnProfile = isOwnProfile(currentUser, profileUser);
        boolean isFollowing = isFollowing(currentUser, profileUser);

        return new ProfileResponse(
                profileUser.getId(),
                profileUser.getPublicId(),
                profileUser.getUsername(),
                profileUser.getBio(),
                profileUser.getAvatarUrl(),
                isOwnProfile,
                isFollowing,
                getFollowersCount(profileUser),
                getFollowingCount(profileUser),
                getUserPosts(profileUser.getUsername()),
                profileUser.getRole()
        );
    }

    @Transactional
    public User updateProfile(
            User user,
            String bio,
            MultipartFile avatar
    ) {

        user.setBio(bio);

        if (avatar != null && !avatar.isEmpty()) {

            String oldAvatarUrl = user.getAvatarUrl();

            String newAvatarUrl =
                    fileStorageService.storeAvatar(avatar);

            user.setAvatarUrl(newAvatarUrl);

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status == STATUS_COMMITTED) {
                                if (oldAvatarUrl != null) {
                                    fileStorageService.deleteAvatarIfExists(oldAvatarUrl);
                                }
                            } else {
                                fileStorageService.deleteAvatarIfExists(newAvatarUrl);
                            }
                        }
                    }
            );
        }

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<FollowerResponse> getFollowers(
            User profileUser,
            User currentUser,
            Pageable pageable) {

        requireFollowListAccess(profileUser, currentUser);
        return followRepository.findFollowers(profileUser, pageable)
                .map(user -> {
                    boolean followedByCurrentUser =
                            currentUser != null &&
                                    followRepository.existsByFollowerAndFollowing(currentUser, user);

                    return followMapper.toFollowerResponse(
                            user,
                            followedByCurrentUser
                    );
                });
    }

    @Transactional(readOnly = true)
    public Page<FollowerResponse> getFollowing(
            User profileUser,
            User currentUser,
            Pageable pageable) {

        requireFollowListAccess(profileUser, currentUser);
        return followRepository.findFollowing(profileUser, pageable)
                .map(user -> {
                    boolean followedByCurrentUser =
                            currentUser != null &&
                                    followRepository.existsByFollowerAndFollowing(currentUser, user);

                    return followMapper.toFollowerResponse(
                            user,
                            followedByCurrentUser
                    );
                });
    }

    private void requireFollowListAccess(User profileUser, User currentUser) {
        if (!isOwnProfile(currentUser, profileUser) && !isFollowing(currentUser, profileUser)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Follow this user to view their followers and following"
            );
        }
    }
}
