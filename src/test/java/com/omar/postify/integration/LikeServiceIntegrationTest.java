package com.omar.postify.integration;

import com.omar.postify.entities.Post;
import com.omar.postify.entities.Role;
import com.omar.postify.entities.User;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.LikeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LikeServiceIntegrationTest {

    @Autowired
    private LikeService likeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Test
    void toggleLikeShouldPersistAndToggleState() {
        User owner = userRepository.save(User.builder()
                .username("owner")
                .email("owner@example.com")
                .password("password123")
                .role(Role.USER)
                .build());

        User actor = userRepository.save(User.builder()
                .username("actor")
                .email("actor@example.com")
                .password("password123")
                .role(Role.USER)
                .build());

        Post post = postRepository.save(Post.builder()
                .user(owner)
                .content("first post")
                .createdAt(LocalDateTime.now())
                .build());

        boolean liked = likeService.toggleLike(post.getId(), actor);
        assertTrue(liked);
        assertEquals(1L, likeService.countLikes(post.getId()));
        assertTrue(likeService.isLikedByUser(post.getId(), actor));

        boolean unliked = likeService.toggleLike(post.getId(), actor);
        assertFalse(unliked);
        assertEquals(0L, likeService.countLikes(post.getId()));
        assertFalse(likeService.isLikedByUser(post.getId(), actor));
    }
}
