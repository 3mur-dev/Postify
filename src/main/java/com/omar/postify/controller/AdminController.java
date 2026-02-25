package com.omar.postify.controller;

import com.omar.postify.entities.AdminLog;
import com.omar.postify.service.AdminLogService;
import com.omar.postify.service.PostService;
import com.omar.postify.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final PostService postService;
    private final UserService userService;
    private final AdminLogService adminLogService;

    @GetMapping
    public String admin() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {

        long userCount = userService.getUserCount();
        long postCount = postService.getPostCount();
        long adminCount = userService.getAdminCount();

        model.addAttribute("userCount", userCount);
        model.addAttribute("postCount", postCount);
        model.addAttribute("adminCount", adminCount);

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String manageUsers(Model model) {

        model.addAttribute("users", userService.getAllUsers());

        return "admin/users";
    }

    @PostMapping("/users/promote/{id}")
    public String promote(@PathVariable Long id, Authentication auth) {
        userService.promoteToAdmin(id);

        adminLogService.logAction(auth.getName(), "Promote User", "User ID: " + id);

        return "redirect:/admin/users";
    }

    @PostMapping("/users/delete/{id}")
    @Transactional
    public String delete(@PathVariable Long id, Authentication auth) {
        userService.deleteUser(id);

        adminLogService.logAction(auth.getName(), "Delete User", "User ID: " + id);

        return "redirect:/admin/users";
    }

    @GetMapping("/logs")
    public String viewLogs(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Page<AdminLog> logPage = adminLogService.getLogs(keyword, page);

        model.addAttribute("logs", logPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("totalLogs", logPage.getTotalElements());
        return "admin/logs";
    }
}