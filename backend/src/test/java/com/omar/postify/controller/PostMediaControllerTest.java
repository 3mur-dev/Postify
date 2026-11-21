package com.omar.postify.controller;

import com.omar.postify.dto.ApiResponse;
import com.omar.postify.dto.PostFeedDto;
import com.omar.postify.service.PostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostMediaControllerTest {

    @Mock private PostService postService;

    @InjectMocks
    private PostController postController;

    @Test
    void createsPostWithMedia() {
        MockMultipartFile media = new MockMultipartFile(
                "media", "test.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        PostFeedDto feedDto = new PostFeedDto(
                1L,
                "pub-1",
                "test",
                List.of(),
                LocalDateTime.now(),
                "member",
                null,
                0L,
                0L,
                false,
                false
        );
        when(postService.createPost(anyString(), anyString(), anyList()))
                .thenReturn(feedDto);

        Principal principal = () -> "member";
        var response = postController.createPost("test", List.of(media), principal);

        verify(postService).createPost(anyString(), anyString(), anyList());
        assertNotNull(response);
        assertEquals(1L, response.data().id());
    }
}
