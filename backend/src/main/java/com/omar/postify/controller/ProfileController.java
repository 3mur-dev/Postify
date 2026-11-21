package com.omar.postify.controller;

import com.omar.postify.dto.*;
import com.omar.postify.entities.User;
import com.omar.postify.service.CurrentUserService;
import com.omar.postify.service.PostService;
import com.omar.postify.service.ProfileService;
import com.omar.postify.service.SavedPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final CurrentUserService currentUserService;
    private final SavedPostService savedPostService;

    @GetMapping("/{username}")
    public ApiResponse<ProfileResponse> viewProfile(
            @PathVariable String username,
            Authentication authentication
    ) {

        User profileUser = profileService.getProfileUser(username);
        User currentUser = authentication != null
                ? currentUserService.resolveCurrentUser(authentication)
                : null;

        ProfileResponse profile = profileService.getProfileResponse(profileUser, currentUser);

        return ApiResponse.success(
                "Profile fetched successfully",
                profile
        );
    }

    @GetMapping("/me")
    public ApiResponse<ProfileResponse> getMyProfile(Authentication authentication) {

        User currentUser =
                currentUserService.resolveCurrentUser(authentication);

        ProfileResponse profile =
                profileService.getProfileResponse(currentUser, currentUser);

        return ApiResponse.success(
                "Profile fetched successfully",
                profile
        );
    }

    @GetMapping("/{username}/saved")
    public PagedResponse<SavedPostResponse> getSavedPosts(
            @PathVariable String username,
            @PageableDefault(size = 20, sort = "savedAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            Authentication authentication
    ) {

        User profileUser = profileService.getProfileUser(username);
        User requester = currentUserService.resolveCurrentUser(authentication);

        Page<SavedPostResponse> savedPosts =
                savedPostService.getSavedPosts(profileUser, requester, pageable);

        return PagedResponse.from(savedPosts);
    }


    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProfileResponse> updateProfile(
            @RequestParam(value = "bio", required = false) String bio,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            Authentication authentication
    ) throws IOException {

        User currentUser =
                currentUserService.resolveCurrentUser(authentication);

        User updatedUser =
                profileService.updateProfile(currentUser, bio, avatar);

        ProfileResponse profile =
                profileService.getProfileResponse(updatedUser, updatedUser);

        return ApiResponse.success(
                "Profile updated successfully",
                profile
        );
    }

    @GetMapping("/me/followers")
    public PagedResponse<FollowerResponse> getFollowers(
            Pageable pageable,
            Authentication authentication) {

        User currentUser = currentUserService.resolveCurrentUser(authentication);

        return PagedResponse.from(
                profileService.getFollowers(
                        currentUser,
                        currentUser,
                        pageable
                )
        );
    }

    @GetMapping("/{username}/followers")
    public PagedResponse<FollowerResponse> getProfileFollowers(
            @PathVariable String username,
            Pageable pageable,
            Authentication authentication) {

        User profileUser = profileService.getProfileUser(username);
        User currentUser = currentUserService.resolveCurrentUser(authentication);

        return PagedResponse.from(
                profileService.getFollowers(profileUser, currentUser, pageable)
        );
    }

    @GetMapping("/{username}/following")
    public PagedResponse<FollowerResponse> getProfileFollowing(
            @PathVariable String username,
            Pageable pageable,
            Authentication authentication) {

        User profileUser = profileService.getProfileUser(username);
        User currentUser = currentUserService.resolveCurrentUser(authentication);

        return PagedResponse.from(
                profileService.getFollowing(profileUser, currentUser, pageable)
        );
    }
}
