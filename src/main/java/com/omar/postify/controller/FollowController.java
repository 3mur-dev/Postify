package com.omar.postify.controller;

import com.omar.postify.entities.Follow;
import com.omar.postify.entities.User;
import com.omar.postify.repository.FollowRepository;
import com.omar.postify.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@Controller
@RequestMapping("/follow")
public class FollowController {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowController(FollowRepository followRepository,
                            UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/{username}")
    public String followOrUnfollow(@PathVariable String username,
                                   Principal principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        User currentUser = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

        User targetUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        // Prevent self-follow
        if (currentUser.getId().equals(targetUser.getId())) {
            return "redirect:/profile/" + username;
        }

        boolean alreadyFollowing = followRepository
                .existsByFollowerAndFollowing(currentUser, targetUser);

        if (alreadyFollowing) {
            // Unfollow
            followRepository.deleteByFollowerAndFollowing(currentUser, targetUser);
        } else {
            // Follow
            Follow follow = new Follow(currentUser, targetUser);
            followRepository.save(follow);
        }
        return "redirect:/profile/" + username;
    }
}