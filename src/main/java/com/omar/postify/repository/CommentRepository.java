package com.omar.postify.repository;

import com.omar.postify.entities.Comment;
import com.omar.postify.entities.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostOrderByCreatedAtAsc(Post post);

    long countByPost(Post post);

    void deleteByPostId(Long postId);
}
