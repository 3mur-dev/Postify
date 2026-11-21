package com.omar.postify.controller;

import com.omar.postify.dto.ApiResponse;
import com.omar.postify.dto.LikeResponse;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.entities.enums.InteractionSource;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.LikeService;
import com.omar.postify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/{publicId}/like")
    public ApiResponse<LikeResponse> toggleLike(@PathVariable String publicId,
                                                @RequestParam InteractionSource source,
                                                Principal principal) {

        return ApiResponse.success("Like toggled successfully",
                likeService.toggleLike(publicId, principal.getName(), source
                )
        );
    }
}