package com.omar.postify.service;

import com.omar.postify.entities.Like;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.repository.LikeRepository;
import com.omar.postify.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;

    public void toggleLike(Long postId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Optional<Like> existingLike = likeRepository.findByUserAndPost(user, post);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get()); // Unlike
        } else {
            Like like = Like.builder()
                    .user(user)
                    .post(post)
                    .build();
            likeRepository.save(like); // Like
        }
    }

    public long countLikes(Post post) {
        return likeRepository.countByPost(post);
    }

    public boolean isLikedByUser(Post post, User user) {

        if (user == null) {
            return false;
        }

        return likeRepository.findByUserAndPost(user, post).isPresent();
    }

}