package com.omar.postify.repository;

import com.omar.postify.entities.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
SELECT c
FROM Comment c
JOIN FETCH c.user
WHERE c.post.id = :postId
ORDER BY c.createdAt ASC
""")
    List<Comment> findByPostIdOrderByCreatedAtAsc(@Param("postId") Long postId);

    long countByPostId(Long postId);

    @Query("""
SELECT c.post.id as postId, COUNT(c) as total
FROM Comment c
WHERE c.post.id IN :postIds
GROUP BY c.post.id
""")
    List<PostCountProjection> countCommentsForPostIds(@Param("postIds") Collection<Long> postIds);

    void deleteByPostId(Long postId);
}
