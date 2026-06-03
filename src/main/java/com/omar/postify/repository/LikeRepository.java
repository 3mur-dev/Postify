package com.omar.postify.repository;

import com.omar.postify.entities.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserIdAndPostId(Long userId, Long postId);

    long countByPostId(Long postId);

    @Query("""
SELECT l.post.id as postId, COUNT(l) as total
FROM Like l
WHERE l.post.id IN :postIds
GROUP BY l.post.id
""")
    List<PostCountProjection> countLikesForPostIds(@Param("postIds") Collection<Long> postIds);

    @Query("""
SELECT l.post.id
FROM Like l
WHERE l.user.id = :userId
AND l.post.id IN :postIds
""")
    List<Long> findLikedPostIdsByUserIdAndPostIds(@Param("userId") Long userId,
                                                  @Param("postIds") Collection<Long> postIds);

    void deleteByPostId(Long postId);

}
