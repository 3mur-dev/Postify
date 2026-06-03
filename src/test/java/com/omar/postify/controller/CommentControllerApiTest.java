package com.omar.postify.controller;

import com.omar.postify.entities.Comment;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.Role;
import com.omar.postify.entities.User;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.CommentService;
import com.omar.postify.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private NotificationService notificationService;

    @Test
    void addCommentShouldReturnUnauthorizedWhenPrincipalIsMissing() throws Exception {
        mockMvc.perform(post("/posts/1/comments")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", "hello"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void getCommentsShouldReturnMappedPayload() throws Exception {
        User user = User.builder()
                .id(3L)
                .username("alice")
                .email("alice@example.com")
                .password("password123")
                .role(Role.USER)
                .avatarUrl("/images/alice.png")
                .build();

        Post post = Post.builder()
                .id(7L)
                .user(user)
                .content("post")
                .build();

        Comment comment = Comment.builder()
                .id(9L)
                .user(user)
                .post(post)
                .content("nice")
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 30))
                .build();

        when(commentService.getCommentsForPost(7L)).thenReturn(List.of(comment));

        mockMvc.perform(get("/posts/7/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].user").value("alice"))
                .andExpect(jsonPath("$[0].avatar").value("/images/alice.png"))
                .andExpect(jsonPath("$[0].content").value("nice"));
    }

    @Test
    void addCommentShouldNotifyOwnerWhenDifferentUserComments() throws Exception {
        User commenter = User.builder()
                .id(1L)
                .username("john")
                .email("john@example.com")
                .password("password123")
                .role(Role.USER)
                .avatarUrl("/images/john.png")
                .build();

        User owner = User.builder()
                .id(2L)
                .username("owner")
                .email("owner@example.com")
                .password("password123")
                .role(Role.USER)
                .build();

        Post post = Post.builder()
                .id(11L)
                .user(owner)
                .content("content")
                .build();

        Comment comment = Comment.builder()
                .id(17L)
                .user(commenter)
                .post(post)
                .content("great post")
                .createdAt(LocalDateTime.of(2026, 2, 1, 9, 0))
                .build();

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(commenter));
        when(commentService.addComment(eq(11L), eq(commenter), eq("great post"))).thenReturn(comment);

        mockMvc.perform(post("/posts/11/comments")
                        .principal(() -> "john")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", "great post"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(17))
                .andExpect(jsonPath("$.user").value("john"))
                .andExpect(jsonPath("$.content").value("great post"));

        verify(notificationService).notifyUser(
                eq(owner),
                eq("comment"),
                eq("john commented on your post"),
                eq("/profile/john")
        );
    }

    @Test
    void addCommentShouldNotNotifyWhenUserCommentsOwnPost() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("john")
                .email("john@example.com")
                .password("password123")
                .role(Role.USER)
                .build();

        Post post = Post.builder()
                .id(11L)
                .user(user)
                .content("content")
                .build();

        Comment comment = Comment.builder()
                .id(18L)
                .user(user)
                .post(post)
                .content("self comment")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(commentService.addComment(eq(11L), eq(user), eq("self comment"))).thenReturn(comment);

        mockMvc.perform(post("/posts/11/comments")
                        .principal(() -> "john")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", "self comment"))
                .andExpect(status().isOk());

        verify(notificationService, never()).notifyUser(any(), any(), any(), any());
    }
}
