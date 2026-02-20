package com.omar.postify.repository;

import com.omar.postify.entities.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findAllByUser_UsernameOrderByCreatedAtDesc(String username);
    List<Post> findAllByOrderByCreatedAtDesc();

    Page<Post> findByContentContainingIgnoreCase(String keyword, Pageable pageable);

    @Query("""
SELECT p FROM Post p\s
WHERE lower(p.content) LIKE lower(concat('%', :keyword, '%'))
""")
    Page<Post> searchPosts(@Param("keyword") String keyword, Pageable pageable);

}