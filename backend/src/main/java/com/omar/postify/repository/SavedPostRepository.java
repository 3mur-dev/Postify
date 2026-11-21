package com.omar.postify.repository;

import com.omar.postify.entities.Post;
import com.omar.postify.entities.SavedPost;
import com.omar.postify.entities.User;
import com.omar.postify.entities.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SavedPostRepository extends JpaRepository<SavedPost, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Optional<SavedPost> findByUserIdAndPostId(Long userId, Long postId);

    @Query("""
        SELECT sp.post.id
        FROM SavedPost sp
        WHERE sp.user.id = :userId
          AND sp.post.id IN :postIds
        """)
    List<Long> findSavedPostIds(
            @Param("userId") Long userId,
            @Param("postIds") Collection<Long> postIds
    );

    @EntityGraph(attributePaths = {"post", "post.user"})
    @Query("""
            select savedPost from SavedPost savedPost
            where savedPost.user = :user
              and savedPost.post.status = :status
            """)
    Page<SavedPost> findActivePostsByUser(
            @Param("user") User user,
            @Param("status") PostStatus status,
            Pageable pageable
    );

    boolean existsByUserAndPost(User user, Post post);

    Optional<SavedPost> findByUserIdAndPostPublicId(Long userId, String postPublicId);

    boolean existsByUserIdAndPostPublicId(Long userId, String postPublicId);

    void deleteByPostPublicId(String postPublicId);
}
