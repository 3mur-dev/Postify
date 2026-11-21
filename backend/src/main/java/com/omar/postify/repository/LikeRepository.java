package com.omar.postify.repository;

import com.omar.postify.dto.PostLikeCount;
import com.omar.postify.entities.Like;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.*;

public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserIdAndPostPublicId(Long userId, String publicId);

    /**
     * Batch count likes for multiple posts. Returns [postId, count] rows.
     */
    @Query("""
SELECT l.post.id AS postId, COUNT(l) AS count
FROM Like l
WHERE l.post.id IN :postIds
GROUP BY l.post.id
""")
    List<PostLikeCount> countByPostIdInGrouped(@Param("postIds") Collection<Long> postIds);

    /**
     * Batch find which posts a user has liked. Returns list of post IDs.
     */
    @Query("SELECT l.post.id FROM Like l WHERE l.user.id = :userId AND l.post.id IN :postIds")
    List<Long> findLikedPostIds(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("""
SELECT FUNCTION('DATE', l.createdAt), COUNT(l)
FROM Like l
WHERE l.createdAt BETWEEN :fromDate AND :toDate
GROUP BY FUNCTION('DATE', l.createdAt)
ORDER BY FUNCTION('DATE', l.createdAt)
""")
    List<Object[]> countLikesGroupedByDay(LocalDateTime fromDate, LocalDateTime toDate);

    boolean existsByUserAndPost(User user, Post savedPost);

    void deleteByPostPublicId(String publicId);

}