package com.omar.postify.controller;

import com.omar.postify.entities.User;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.LikeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/posts")
public class LikeController {

    private final LikeService likeService;
    private final UserRepository userRepository;

    @PostMapping("/{postId}/like")
    public String toggleLike(@PathVariable Long postId,
                             Principal principal,
                             HttpServletRequest request) {

        if (principal == null) return "redirect:/login";

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow();

        likeService.toggleLike(postId, user);

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }
}
