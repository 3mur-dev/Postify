package com.omar.postify.controller;

import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.service.CommentService;
import com.omar.postify.service.LikeService;
import com.omar.postify.service.PostService;
import com.omar.postify.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;
import java.util.stream.IntStream;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final PostService postService;
    private final UserService userService;
    private final LikeService likeService;
    private final CommentService commentService;

    @GetMapping("/")
    public String home(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            Model model,
            Principal principal) {

        int currentPage = Math.max(page, 0);
        int pageSize = Math.max(size, 1);
        User currentUser = null;

        if (principal != null) {
            currentUser = userService.getUserByUsername(principal.getName());
        }

        Page<Post> posts = postService.getPosts(keyword, currentPage, pageSize);

        int totalPages = posts.getTotalPages();
        List<Integer> pageNumbers = totalPages > 0
                ? IntStream.range(0, totalPages).boxed().toList()
                : List.of();

        model.addAttribute("posts", posts);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageTitle", "Home - Postify");
        model.addAttribute("pageNumbers", pageNumbers);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("likeService", likeService);
        model.addAttribute("commentService", commentService);

        return "home";
    }
}
