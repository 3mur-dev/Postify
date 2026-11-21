package com.omar.postify.service;

import com.omar.postify.entities.Follow;
import com.omar.postify.entities.User;
import com.omar.postify.event.UserFollowedEvent;
import com.omar.postify.exception.BadRequestException;
import com.omar.postify.exception.ResourceNotFoundException;
import com.omar.postify.repository.FollowRepository;
import com.omar.postify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public boolean toggleFollow(String currentUsername, String targetUsername) {

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User targetUser = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new BadRequestException("You cannot follow yourself");
        }

        if (followRepository.existsByFollowerAndFollowing(currentUser, targetUser)) {
            followRepository.deleteByFollowerAndFollowing(currentUser, targetUser);
            return false; // unfollowed
        }

        followRepository.save(
                Follow.builder()
                        .follower(currentUser)
                        .following(targetUser)
                        .build()
        );

        eventPublisher.publishEvent(
                new UserFollowedEvent(
                        currentUser.getId(),
                        targetUser.getId()
                )
        );
        return true; // followed
    }
}