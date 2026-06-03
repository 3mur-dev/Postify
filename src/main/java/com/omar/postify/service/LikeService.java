package com.omar.postify.service;

import com.omar.postify.entities.Like;
import com.omar.postify.entities.User;
import com.omar.postify.repository.LikeRepository;
import com.omar.postify.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;

    // Toggle like and return new status
    @Transactional
    public boolean toggleLike(Long postId, User user) {
        Optional<Like> existing = likeRepository.findByUserIdAndPostId(user.getId(), postId);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            return false;
        } else {
            Like like = Like.builder()
                    .user(user)
                    .post(postRepository.getReferenceById(postId))
                    .build();
            likeRepository.save(like);
            return true;
        }
    }

    public long countLikes(Long postId) {
        return likeRepository.countByPostId(postId);
    }

    public boolean isLikedByUser(Long postId, User user) {
        if (user == null) return false;
        return likeRepository.findByUserIdAndPostId(user.getId(), postId).isPresent();
    }
}
