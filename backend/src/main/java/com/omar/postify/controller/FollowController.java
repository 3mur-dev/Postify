package com.omar.postify.controller;

import com.omar.postify.dto.ApiResponse;
import com.omar.postify.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{username}")
    public ApiResponse<Boolean> toggleFollow(@PathVariable("username") String targetUsername, Principal principal) {

        return ApiResponse.success(
                "Follow toggled successfully",
                followService.toggleFollow(principal.getName(), targetUsername)
        );
    }
}
