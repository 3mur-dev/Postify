package com.omar.postify.repository;

import com.omar.postify.dto.PostFeedDto;
import com.omar.postify.entities.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findAllByUser_UsernameOrderByCreatedAtDesc(String username);
    List<Post> findAllByOrderByCreatedAtDesc();
    Optional<Post> findByIdAndUser_Username(Long id, String username);

    @Query("""
SELECT p
FROM Post p
JOIN FETCH p.user
WHERE p.id = :postId
""")
    Optional<Post> findWithUserById(@Param("postId") Long postId);

    Page<Post> findByContentContainingIgnoreCase(String keyword, Pageable pageable);

    @Query("""
SELECT new com.omar.postify.dto.PostFeedDto(
    p.id,
    p.content,
    p.imageUrl,
    p.user.id,
    p.user.username,
    p.user.avatarUrl,
    p.createdAt,
    0L,
    0L,
    false
)
FROM Post p
WHERE (:keyword IS NULL OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
ORDER BY p.createdAt DESC
""")
    Page<PostFeedDto> searchPosts(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
SELECT new com.omar.postify.dto.PostFeedDto(
    p.id,
    p.content,
    p.imageUrl,
    p.user.id,
    p.user.username,
    p.user.avatarUrl,
    p.createdAt,
    0L,
    0L,
    false
)
FROM Post p
ORDER BY p.createdAt DESC
""")
    Page<PostFeedDto> findAllPosts(Pageable pageable);

}
