package com.omar.postify.service;

import com.omar.postify.dto.UserDto;
import com.omar.postify.entities.User;
import com.omar.postify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(UserDto userDto, BindingResult bindingResult) {

        if (userRepository.existsByUsername(userDto.getUsername())) {
            bindingResult.rejectValue(
                    "username",
                    "error.user",
                    "Username already exists"
            );
        }
        if (userRepository.existsByEmail(userDto.getEmail())) {
            bindingResult.rejectValue(
                    "email",
                    "error.user",
                    "Email already exists"
            );
        }

        if (bindingResult.hasErrors()) {
            return null;
        }

        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        User savedUser = userRepository.save(user);

       return savedUser;

    }
}