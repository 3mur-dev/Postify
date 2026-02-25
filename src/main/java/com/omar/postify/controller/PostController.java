package com.omar.postify.controller;

import com.omar.postify.entities.User;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.PostService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@Controller
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UserRepository userRepository;


    @PostMapping("/create")
    public String createPost(@RequestParam(required = false) String content,
                             @RequestParam(value = "image", required = false) MultipartFile image,
                             Principal principal) {

        if (principal == null) return "redirect:/login";

        postService.createPost(content, principal.getName(), image);
        return "redirect:/profile/" + principal.getName();
    }

    @PostMapping("/delete/{postId}")
    public String deletePost(@PathVariable Long postId, Principal principal) {

        if (principal == null) return "redirect:/login";

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        postService.deletePost(postId, user);

        // Redirect back to the page where the request came from
        return "redirect:/";
    }
}