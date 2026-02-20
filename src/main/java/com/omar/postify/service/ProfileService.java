package com.omar.postify.service;

import com.omar.postify.dto.UserProfileDto;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.repository.FollowRepository;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FollowRepository followRepository;

    private static final Path AVATAR_UPLOAD_DIR =
            Paths.get(System.getProperty("user.dir"), "uploads", "avatars");

    public User getProfileUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getCurrentUser(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByUsername(principal.getName()).orElse(null);
    }

    public boolean isOwnProfile(User currentUser, User profileUser) {
        return currentUser != null && currentUser.getId().equals(profileUser.getId());
    }

    public boolean isFollowing(User currentUser, User profileUser) {
        if (currentUser == null || isOwnProfile(currentUser, profileUser)) return false;
        return followRepository.existsByFollowerAndFollowing(currentUser, profileUser);
    }

    public List<Post> getPosts(String username) {
        return postRepository.findAllByUser_UsernameOrderByCreatedAtDesc(username);
    }

    public long getFollowersCount(User profileUser) {
        return followRepository.countByFollowing(profileUser);
    }

    public long getFollowingCount(User profileUser) {
        return followRepository.countByFollower(profileUser);
    }

    public User updateProfile(User user, String bio, MultipartFile avatar) throws IOException {

        // Update bio
        user.setBio(bio);

        // Handle avatar
        if (avatar != null && !avatar.isEmpty()) {
            String originalName = StringUtils.cleanPath(Objects.requireNonNull(avatar.getOriginalFilename()));
            if (originalName.contains("..")) throw new IllegalArgumentException("Invalid file path");

            String extension = "";
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0) extension = originalName.substring(dotIndex);

            Files.createDirectories(AVATAR_UPLOAD_DIR);
            String fileName = UUID.randomUUID() + extension;
            Path target = AVATAR_UPLOAD_DIR.resolve(fileName).normalize();
            if (!target.startsWith(AVATAR_UPLOAD_DIR)) throw new IllegalArgumentException("Invalid file path");

            avatar.transferTo(target.toFile());
            user.setAvatarUrl("/images/avatars/" + fileName);
        }

        return userRepository.save(user);
    }

    public List<User> searchUsers(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        return userRepository.findByUsernameContainingIgnoreCase(query.trim());
    }
}