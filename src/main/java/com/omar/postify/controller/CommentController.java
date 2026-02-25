package com.omar.postify.controller;

import com.omar.postify.entities.Comment;
import com.omar.postify.entities.User;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.CommentService;
import com.omar.postify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/posts")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @GetMapping("/{postId}/comments")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getComments(@PathVariable Long postId) {
        List<Comment> comments = commentService.getCommentsForPost(postId);

        List<Map<String, Object>> dto = comments.stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "user", c.getUser().getUsername(),
                        "avatar", c.getUser().getAvatarUrl(),
                        "content", c.getContent(),
                        "createdAt", FORMATTER.format(c.getCreatedAt())
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{postId}/comments")
    @ResponseBody
    public ResponseEntity<?> addComment(@PathVariable Long postId,
                                        @RequestParam String content,
                                        Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.<String, Object>of("error", "Unauthorized"));
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Comment comment = commentService.addComment(postId, user, content);

        // notify post owner if different
        User postOwner = comment.getPost().getUser();
        if (!postOwner.getId().equals(user.getId())) {
            notificationService.notifyUser(
                    postOwner,
                    "comment",
                    user.getUsername() + " commented on your post",
                    "/profile/" + user.getUsername()
            );
        }

        Map<String, Object> dto = Map.<String, Object>of(
                "id", comment.getId(),
                "user", user.getUsername(),
                "avatar", user.getAvatarUrl(),
                "content", comment.getContent(),
                "createdAt", FORMATTER.format(comment.getCreatedAt())
        );

        return ResponseEntity.ok(dto);
    }
}
