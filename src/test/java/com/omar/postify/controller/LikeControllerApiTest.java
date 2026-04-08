package com.omar.postify.controller;

import com.omar.postify.entities.Post;
import com.omar.postify.entities.Role;
import com.omar.postify.entities.User;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.LikeService;
import com.omar.postify.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LikeController.class)
@AutoConfigureMockMvc(addFilters = false)
class LikeControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LikeService likeService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private NotificationService notificationService;

    @Test
    void toggleLikeShouldReturnUnauthorizedWhenPrincipalIsMissing() throws Exception {
        mockMvc.perform(post("/posts/10/like"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void toggleLikeShouldReturnCountAndTriggerNotificationWhenLiked() throws Exception {
        User actor = User.builder()
                .id(1L)
                .username("john")
                .email("john@example.com")
                .password("password123")
                .role(Role.USER)
                .build();

        User owner = User.builder()
                .id(2L)
                .username("owner")
                .email("owner@example.com")
                .password("password123")
                .role(Role.USER)
                .build();

        Post post = Post.builder()
                .id(10L)
                .user(owner)
                .content("hello")
                .build();

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(actor));
        when(likeService.toggleLike(10L, actor)).thenReturn(true);
        when(likeService.countLikes(10L)).thenReturn(4L);
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        mockMvc.perform(post("/posts/10/like").principal(() -> "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.count").value(4));

        verify(notificationService).notifyUser(
                eq(owner),
                eq("like"),
                eq("john liked your post"),
                eq("/profile/john")
        );
    }

    @Test
    void toggleLikeShouldNotNotifyWhenUnliked() throws Exception {
        User actor = User.builder()
                .id(1L)
                .username("john")
                .email("john@example.com")
                .password("password123")
                .role(Role.USER)
                .build();

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(actor));
        when(likeService.toggleLike(10L, actor)).thenReturn(false);
        when(likeService.countLikes(10L)).thenReturn(0L);

        mockMvc.perform(post("/posts/10/like").principal(() -> "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.count").value(0));

        verify(postRepository, never()).findById(any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any());
    }
}
