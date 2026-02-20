package com.omar.postify.controller;

import com.omar.postify.entities.User;
import com.omar.postify.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/search")
public class SearchController {

    private final ProfileService profileService;

    @GetMapping
    public String search(@RequestParam("q") String query, Model model) {
        List<User> results = profileService.searchUsers(query);
        model.addAttribute("results", results);
        model.addAttribute("query", query);
        return "search";
    }
}

