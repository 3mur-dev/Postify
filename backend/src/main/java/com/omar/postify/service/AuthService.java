package com.omar.postify.service;

import com.omar.postify.dto.ApiResponse;
import com.omar.postify.dto.AuthResponse;
import com.omar.postify.dto.LoginRequest;
import com.omar.postify.dto.RegisterRequest;
import com.omar.postify.entities.enums.Role;
import com.omar.postify.entities.User;
import com.omar.postify.entities.enums.UserStatus;
import com.omar.postify.event.SystemNotificationEvent;
import com.omar.postify.exception.BadCredentialsCustomException;
import com.omar.postify.exception.ConflictException;
import com.omar.postify.exception.InvalidUserDataException;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validatePasswords(request);

        String username = request.username().trim();
        String email = request.email().trim();

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);

        /*
         * Rely on the database unique constraint to enforce username/email uniqueness.
         * This eliminates the TOCTOU race condition where two concurrent registrations
         * could both pass a check-then-act uniqueness check before either saves.
         * The unique constraint guarantees only one row wins; the other gets a
         * DataIntegrityViolationException that we translate to a ConflictException.
         */
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("username") || msg.contains("USERNAME"))) {
                throw new ConflictException("Username already exists");
            }
            if (msg != null && (msg.contains("email") || msg.contains("EMAIL"))) {
                throw new ConflictException("Email already exists");
            }
            throw new ConflictException("User details already exist");
        }

        eventPublisher.publishEvent(new SystemNotificationEvent(
                "Welcome to Postify, " + user.getUsername() + "!"
                , user.getId())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        return AuthResponse.of(token);
    }

    public AuthResponse login(LoginRequest request) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);

            return AuthResponse.of(token);
        } catch (AuthenticationException ex) {
            throw new BadCredentialsCustomException("Bad credentials");
        }
    }

    private void validatePasswords(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
    }
}