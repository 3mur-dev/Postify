package com.omar.postify.integration;

import com.omar.postify.entities.Comment;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.Role;
import com.omar.postify.entities.User;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommentServiceIntegrationTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Test
    void addCommentShouldSaveTrimmedBodyAndBeQueryable() {
        User owner = userRepository.save(User.builder()
                .username("owner2")
                .email("owner2@example.com")
                .password("password123")
                .role(Role.USER)
                .build());

        User commenter = userRepository.save(User.builder()
                .username("commenter2")
                .email("commenter2@example.com")
                .password("password123")
                .role(Role.USER)
                .build());

        Post post = postRepository.save(Post.builder()
                .user(owner)
                .content("another post")
                .createdAt(LocalDateTime.now())
                .build());

        Comment saved = commentService.addComment(post.getId(), commenter, "  nice post  ");
        assertEquals("nice post", saved.getContent());
        assertEquals(1L, commentService.countComments(post.getId()));

        List<Comment> comments = commentService.getCommentsForPost(post.getId());
        assertEquals(1, comments.size());
        assertEquals("nice post", comments.get(0).getContent());
    }

    @Test
    void addCommentShouldRejectBlankContent() {
        User owner = userRepository.save(User.builder()
                .username("owner3")
                .email("owner3@example.com")
                .password("password123")
                .role(Role.USER)
                .build());

        User commenter = userRepository.save(User.builder()
                .username("commenter3")
                .email("commenter3@example.com")
                .password("password123")
                .role(Role.USER)
                .build());

        Post post = postRepository.save(Post.builder()
                .user(owner)
                .content("post body")
                .createdAt(LocalDateTime.now())
                .build());

        assertThrows(IllegalArgumentException.class, () ->
                commentService.addComment(post.getId(), commenter, "   "));
    }
}
