package com.omar.postify.controller;

import com.omar.postify.dto.ApiResponse;
import com.omar.postify.dto.CommentDto;
import com.omar.postify.dto.CreateCommentRequest;
import com.omar.postify.entities.enums.InteractionSource;
import com.omar.postify.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/{publicId}/comments")
    public ApiResponse<List<CommentDto>> getComments(@PathVariable String publicId) {
        return ApiResponse.success(
                "comments loaded successfully",
                commentService.getComments(publicId)
        );
    }

    @PostMapping("/{publicId}/comments")
    public ApiResponse<CommentDto> addComment(
            @PathVariable String publicId,
            @Valid @RequestBody CreateCommentRequest request,
            @RequestParam InteractionSource source,
            Principal principal) {

        return ApiResponse.success(
                "Comment added successfully",
                commentService.addComment(
                        publicId,
                        principal.getName(),
                        request.content(),
                        source
                )
        );
    }
}