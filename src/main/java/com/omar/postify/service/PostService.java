package com.omar.postify.service;

import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.exception.InvalidPostException;
import com.omar.postify.exception.PostNotFoundException;
import com.omar.postify.exception.UnauthorizedActionException;
import com.omar.postify.repository.CommentRepository;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    private static final Path POST_UPLOAD_DIR =
            Paths.get(System.getProperty("user.dir"), "uploads", "posts");

    public void createPost(String content, String username, MultipartFile image) {

        boolean hasContent = content != null && !content.trim().isEmpty();
        boolean hasImage = image != null && !image.isEmpty();

        if (!hasContent && hasImage) {
            throw new InvalidPostException("Image posts must include a caption.");
        }

        if (!hasContent && !hasImage) {
            throw new InvalidPostException("Post must contain text or an image.");
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

    @Transactional
    public void deletePost(Long postId, User currentUser) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        // Security check
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException(
                    "You cannot delete someone else's post"
            );
        }

        deletePostImageIfExists(post);

        commentRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    private void deletePostImageIfExists(Post post) {

        String imageUrl = post.getImageUrl();

        if (imageUrl == null || !imageUrl.startsWith("/images/posts/")) {
            return;
        }

        String filename = imageUrl.substring("/images/posts/".length());
        Path target = POST_UPLOAD_DIR.resolve(filename).normalize();

        if (!target.startsWith(POST_UPLOAD_DIR)) {
            return; // Prevent path traversal
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            // Log instead of ignore
            log.warn("Failed to delete image file: {}", target, e);
        }
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

    public long getPostCount() {
        return postRepository.count();
    }
}
