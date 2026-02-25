package com.omar.postify.service;

import com.omar.postify.entities.Comment;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.repository.CommentRepository;
import com.omar.postify.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Transactional
    public Comment addComment(Long postId, User user, String content) {
        if (user == null) throw new RuntimeException("Unauthorized");
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment cannot be empty");
        }
        String body = content.trim();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(body)
                .build();

        return commentRepository.save(comment);
    }

    public List<Comment> getCommentsForPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        return commentRepository.findByPostOrderByCreatedAtAsc(post);
    }

    public long countComments(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        return commentRepository.countByPost(post);
    }
}
