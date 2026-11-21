package com.omar.postify.service;

import com.omar.postify.entities.User;
import com.omar.postify.exception.InsufficientAuthenticationException;
import com.omar.postify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User resolveCurrentUser(Authentication authentication) {

        if (authentication == null ||
                authentication.getName() == null ||
                authentication.getName().isBlank()) {
            throw new InsufficientAuthenticationException("User not found");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new InsufficientAuthenticationException("User not found"));
    }

    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        return userRepository.findByUsername(authentication.getName());
    }
}