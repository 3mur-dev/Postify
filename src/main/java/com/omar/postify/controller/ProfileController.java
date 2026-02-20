package com.omar.postify.controller;

import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.repository.FollowRepository;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.util.StringUtils;

@Controller
@RequiredArgsConstructor
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{username}")
    public String viewProfile(@PathVariable String username, Model model, Principal principal) {

        User profileUser = profileService.getProfileUser(username);
        User currentUser = profileService.getCurrentUser(principal);

        boolean isOwnProfile = profileService.isOwnProfile(currentUser, profileUser);
        boolean isFollowing = profileService.isFollowing(currentUser, profileUser);

        List<Post> posts = profileService.getPosts(username);
        long followersCount = profileService.getFollowersCount(profileUser);
        long followingCount = profileService.getFollowingCount(profileUser);

        model.addAttribute("profile", profileUser);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isOwnProfile", isOwnProfile);
        model.addAttribute("isFollowing", isFollowing);
        model.addAttribute("posts", posts);
        model.addAttribute("followersCount", followersCount);
        model.addAttribute("followingCount", followingCount);

        return "profile";
    }

    @GetMapping("/edit")
    public String editProfile(Model model, Principal principal) {
        User user = profileService.getCurrentUser(principal);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        model.addAttribute("currentUsername", user.getUsername());
        return "profile-edit";
    }

    @PostMapping("/edit")
    public String updateProfile(@ModelAttribute("user") User updatedUser,
                                @RequestParam(value = "avatar", required = false) MultipartFile avatar,
                                Principal principal) throws IOException {

        User currentUser = profileService.getCurrentUser(principal);
        if (currentUser == null) return "redirect:/login";

        profileService.updateProfile(currentUser, updatedUser.getBio(), avatar);
        return "redirect:/profile/" + currentUser.getUsername();
    }
}