package com.omar.postify.controller;

import com.omar.postify.dto.UserDto;
import com.omar.postify.entities.User;
import com.omar.postify.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class RegisterController {

    private final AuthService authService;

    // ==========================
    // SHOW REGISTER PAGE
    // ==========================
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("user", new UserDto());
        return "register";
    }

    // ==========================
    // HANDLE REGISTER
    // ==========================
    @PostMapping("/register/add")
    public String registerUser(
            @Valid @ModelAttribute("user") UserDto userDto,
            BindingResult bindingResult,
            Model model
    ) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.user", "Passwords do not match");
            return "register";
        }

        User savedUser = authService.registerUser(userDto, bindingResult);

        // 🚨 CHECK AGAIN
        if (bindingResult.hasErrors()) {
            return "register";
        }

        return "redirect:/login?success";
    }
}
