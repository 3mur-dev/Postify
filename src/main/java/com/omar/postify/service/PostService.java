package com.omar.postify.service;

import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    private static final Path POST_UPLOAD_DIR =
            Paths.get(System.getProperty("user.dir"), "uploads", "posts");

    public void createPost(String content, String username, MultipartFile image) {

        boolean hasContent = content != null && !content.trim().isEmpty();
        boolean hasImage = image != null && !image.isEmpty();

        if (!hasContent && !hasImage) {
            throw new RuntimeException("Post cannot be empty");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post.PostBuilder builder = Post.builder()
                .content(hasContent ? content.trim() : null)
                .createdAt(LocalDateTime.now())
                .user(user);

        if (hasImage) {
            String imageUrl = storeImage(image);
            builder.imageUrl(imageUrl);
        }

        Post post = builder.build();

        postRepository.save(post);
    }

    public Page<Post> getPosts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (keyword != null && !keyword.isBlank()) {
            return postRepository.searchPosts(keyword, pageable);
        }

        return postRepository.findAll(pageable);
    }

    public void deletePost(Long postId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Only the author can delete
        if (!post.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You cannot delete someone else's post");
        }

        // Delete attached image if any
        if (post.getImageUrl() != null && post.getImageUrl().startsWith("/images/posts/")) {
            String filename = post.getImageUrl().substring("/images/posts/".length());
            Path target = POST_UPLOAD_DIR.resolve(filename).normalize();
            if (target.startsWith(POST_UPLOAD_DIR)) {
                try {
                    Files.deleteIfExists(target);
                } catch (IOException ignored) {
                    // best effort; don't block delete
                }
            }
        }

        postRepository.delete(post);
    }

    private String storeImage(MultipartFile image) {
        String originalName = StringUtils.cleanPath(Objects.requireNonNull(image.getOriginalFilename()));
        if (originalName.contains("..")) throw new IllegalArgumentException("Invalid file path");

        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image uploads are allowed");
        }

        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) extension = originalName.substring(dotIndex);

        try {
            Files.createDirectories(POST_UPLOAD_DIR);
            String fileName = UUID.randomUUID() + extension;
            Path target = POST_UPLOAD_DIR.resolve(fileName).normalize();
            if (!target.startsWith(POST_UPLOAD_DIR)) throw new IllegalArgumentException("Invalid file path");

            image.transferTo(target.toFile());
            return "/images/posts/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store image", e);
        }
    }
}
