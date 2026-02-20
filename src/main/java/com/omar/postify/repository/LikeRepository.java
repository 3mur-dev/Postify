package com.omar.postify.repository;

import com.omar.postify.entities.Like;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserAndPost(User user, Post post);

    long countByPost(Post post);
}

