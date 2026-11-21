package com.omar.postify.controller;

import com.omar.postify.dto.ApiResponse;
import com.omar.postify.dto.PostFeedDto;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.entities.enums.InteractionSource;
import com.omar.postify.exception.ResourceNotFoundException;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.PostService;
import com.omar.postify.service.SavedPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final SavedPostService savedPostService;
    private final PostService postService;

    @GetMapping("/{publicId}")
    public ApiResponse<PostFeedDto> getPostById(@PathVariable String publicId) {
        PostFeedDto postFeedDto = postService.getPostById(publicId);
        return ApiResponse.success(
                "Post retrieved successfully",
                postFeedDto
        );
    }

    @PostMapping()
    public ApiResponse<PostFeedDto> createPost(@RequestParam(required = false) String content,
                                               @RequestParam(value = "media", required = false) List<MultipartFile> media,
                                               Principal principal) {
        return ApiResponse.success(
                "Post created successfully",
                postService.createPost(content, principal.getName(), media)
        );
    }
    @PutMapping("/{publicId}")
    public ApiResponse<PostFeedDto> updatePost(@PathVariable String publicId,
                                               @RequestParam String content,
                                               Principal principal) {
        return ApiResponse.success(
                "Post updated successfully",
                postService.updatePost(publicId, content, principal)
        );
    }

    @DeleteMapping("/{publicId}")
    public ApiResponse<Void> deletePost(@PathVariable String publicId, Principal principal) {

        postService.deletePost(publicId, principal);

        return ApiResponse.success(
                "Post deleted successfully",
                null
        );
    }

    @PostMapping("/{publicId}/save")
    public ApiResponse<Void> savePost(@PathVariable String publicId,
                                       @RequestParam InteractionSource source,
                                       Principal principal) {

        savedPostService.savePost(publicId, principal, source);

        return ApiResponse.success(
                "Post saved successfully",
                null
        );
    }

    @DeleteMapping("/{publicId}/unsave")
    public ApiResponse<Void> unsavePost(@PathVariable String publicId,
                                         @RequestParam InteractionSource source,
                                         Principal principal) {

        savedPostService.unsavePost(publicId, principal);

        return ApiResponse.success(
                "Post unsaved successfully",
                null
        );
    }
}