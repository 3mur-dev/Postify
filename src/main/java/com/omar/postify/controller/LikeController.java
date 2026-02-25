package com.omar.postify.controller;

import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.LikeService;
import com.omar.postify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/posts")
public class LikeController {

    private final LikeService likeService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final NotificationService notificationService;

    @PostMapping("/{postId}/like")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long postId,
                                                          Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow();

        boolean liked = likeService.toggleLike(postId, user);
        long count = likeService.countLikes(postId);

        if (liked) {
            Post post = postRepository.findById(postId)
                    .orElse(null);
            if (post != null && !post.getUser().getId().equals(user.getId())) {
                notificationService.notifyUser(
                        post.getUser(),
                        "like",
                        user.getUsername() + " liked your post",
                        "/profile/" + user.getUsername()
                );
            }
        }

        return ResponseEntity.ok(Map.<String, Object>of(
                "liked", liked,
                "count", count
        ));
    }
}
