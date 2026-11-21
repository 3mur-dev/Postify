package com.omar.postify.repository;

import com.omar.postify.dto.PostCommentCount;
import com.omar.postify.dto.CommentDto;
import com.omar.postify.entities.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
    SELECT new com.omar.postify.dto.CommentDto(
        c.id,
        c.content,
        c.createdAt,
        u.publicId,
        u.username,
        u.avatarUrl,
        p.publicId
    )
    FROM Comment c
    JOIN c.user u
    JOIN c.post p
    WHERE p.publicId = :publicId
      AND p.status = 'ACTIVE'
      AND u.status = 'ACTIVE'
    ORDER BY c.createdAt ASC
    """)
    List<CommentDto> findDtosByPublicIdOrderByCreatedAtAsc(
            @Param("publicId") String publicId
    );

    /**
     * Batch count comments for multiple posts. Returns [postId, count] rows.
     */
    @Query("""
    SELECT c.post.id AS postId, COUNT(c) AS count
    FROM Comment c
    WHERE c.post.id IN :postIds
    GROUP BY c.post.id
""")
    List<PostCommentCount> countByPostIdInGrouped(@Param("postIds") Collection<Long> postIds);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);


    @Query("""
SELECT FUNCTION('DATE', c.createdAt), COUNT(c)
FROM Comment c
WHERE c.createdAt BETWEEN :fromDate AND :toDate
GROUP BY FUNCTION('DATE', c.createdAt)
ORDER BY FUNCTION('DATE', c.createdAt)
""")
    List<Object[]> countCommentsGroupedByDay(LocalDateTime fromDate, LocalDateTime toDate);

    void deleteByPostPublicId(String publicId);

    @Query("""
select c from Comment c
where c.post.id = :postId
order by c.createdAt asc

""")
    List<Comment> findByPostIdOrderByCreatedAtAsc(@Param("postId") Long postId);
}
