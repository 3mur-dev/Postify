package com.omar.postify.repository;

import com.omar.postify.dto.PostFeedBaseDto;
import com.omar.postify.dto.ProfilePostBaseDto;
import com.omar.postify.dto.ProfilePostDto;
import com.omar.postify.entities.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PostRepository
        extends JpaRepository<Post, Long>,
        JpaSpecificationExecutor<Post> {

    @Query("""
            SELECT new com.omar.postify.dto.ProfilePostBaseDto(
                p.id,
                p.publicId,
                p.content,
                p.createdAt,
                p.likeCount,
                p.commentCount,
                p.user.username
            )
            FROM Post p
            WHERE p.user.username = :username
                        AND p.status = 'ACTIVE'
            ORDER BY p.createdAt DESC
            """)
    List<ProfilePostBaseDto> findProfilePostsByUsername(
            @Param("username") String username
    );

    long countByCreatedAtBetween(
            LocalDateTime from,
            LocalDateTime to
    );

    @Query("""
                SELECT FUNCTION('DATE', p.createdAt), COUNT(p)
                FROM Post p
                WHERE p.createdAt BETWEEN :fromDate AND :toDate
                GROUP BY FUNCTION('DATE', p.createdAt)
                ORDER BY FUNCTION('DATE', p.createdAt)
            """)
    List<Object[]> countPostsGroupedByDay(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
                SELECT DISTINCT p
                FROM Post p
                JOIN FETCH p.user
                LEFT JOIN FETCH p.media
                WHERE p.publicId = :publicId AND p.status = 'ACTIVE'
            """)
    Optional<Post> findByPublicIdWithUserAndStatusIsActive(String publicId);

    @Query("""
            select distinct post from Post post
            join fetch post.user
            left join fetch post.media
            where post.publicId = :publicId
            """)
    Optional<Post> findByPublicIdWithUserAndMedia(String publicId);

    boolean existsByPublicIdAndStatus(String publicId, com.omar.postify.entities.enums.PostStatus status);

    @Query("""
                SELECT p.id
                FROM Post p
                WHERE p.user.id IN :authorIds
                  AND p.id NOT IN :excludedPostIds
                  AND p.status = 'ACTIVE'
                ORDER BY p.createdAt DESC
            """)
    List<Long> findPostIdsByUsers(
            @Param("authorIds") Collection<Long> authorIds,
            @Param("excludedPostIds") Collection<Long> excludedPostIds,
            Pageable pageable
    );


    @Query("""
                SELECT p.id
                FROM Post p
                WHERE p.user.id NOT IN :excludedAuthorIds
                  AND p.id NOT IN :excludedPostIds
                  AND p.status = 'ACTIVE'
                  AND NOT EXISTS (
                      SELECT pt.id
                      FROM PostTopic pt
                      WHERE pt.post.id = p.id
                        AND pt.topic.id IN :excludedTopicIds
                  )
                ORDER BY p.createdAt DESC
            """)
    List<Long> findExplorationPostIds(
            @Param("excludedAuthorIds") Collection<Long> excludedAuthorIds,
            @Param("excludedPostIds") Collection<Long> excludedPostIds,
            @Param("excludedTopicIds") Collection<Long> excludedTopicIds,
            Pageable pageable
    );

    @Query("""
                SELECT p.id
                FROM Post p
                WHERE p.user.id NOT IN :excludedAuthorIds
                  AND p.id NOT IN :excludedPostIds
                  AND p.status = 'ACTIVE'
                ORDER BY p.createdAt DESC
            """)
    List<Long> findExplorationPostIdsWithoutTopics(
            @Param("excludedAuthorIds") Collection<Long> excludedAuthorIds,
            @Param("excludedPostIds") Collection<Long> excludedPostIds,
            Pageable pageable
    );

    @Query("""
                SELECT COUNT(pt)
                FROM PostTopic pt
                WHERE pt.post.id = :postId
            """)
    int countTopicsByPostId(
            @Param("postId") Long postId
    );

    @Query("""
                SELECT new com.omar.postify.dto.PostFeedBaseDto(
                    p.id,
                    p.publicId,
                    p.content,
                    p.createdAt,
                    p.user.username,
                    p.user.avatarUrl
                )
                FROM Post p
                WHERE p.id IN :postIds
                AND p.status = 'ACTIVE'
            """)
    List<PostFeedBaseDto> findFeedPostsByIds(
            @Param("postIds") List<Long> postIds
    );

    @Query("""
                SELECT p.id
                FROM Post p
                WHERE p.user.id = :userId
           """)
    Set<Long> findIdsByUserId(@Param("userId") Long userId);

    @Query("""
    SELECT DISTINCT p
    FROM Post p
    LEFT JOIN FETCH p.media
    WHERE NOT EXISTS (
        SELECT 1
        FROM PostTopic pt
        WHERE pt.post = p
    )
    """)
    List<Post> findPostsWithoutTopics();

    @Query("""
            SELECT DISTINCT p
            FROM Post p
            LEFT JOIN FETCH p.media
            WHERE p.id = :postId
            """)
    java.util.Optional<Post> findByIdWithMedia(@Param("postId") Long postId);

    @Query("""
select new com.omar.postify.dto.PostFeedBaseDto(
                    p.id,
                    p.publicId,
                    p.content,
                    p.createdAt,
                    p.user.username,
                    p.user.avatarUrl
                )
                FROM Post p
                WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))
                AND p.status = 'ACTIVE'
                order by p.createdAt desc
            """)
    List<PostFeedBaseDto> searchPostsByContent(@Param("query") String query);

    /**
     * Atomically increment the like count for a post by its primary key.
     * Returns the updated likeCount value.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
    int incrementLikeCount(@Param("postId") Long postId);

    /**
     * Atomically decrement the like count for a post by its primary key.
     * Returns the updated likeCount value.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :postId")
    int decrementLikeCount(@Param("postId") Long postId);

    /**
     * Atomically increment the comment count for a post by its primary key.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    int incrementCommentCount(@Param("postId") Long postId);

    /**
     * Select the current likeCount for a post by ID.
     */
    @Query("SELECT p.likeCount FROM Post p WHERE p.id = :postId")
    Integer selectLikeCountById(@Param("postId") Long postId);

    /**
     * Select the current commentCount for a post by ID.
     */
    @Query("SELECT p.commentCount FROM Post p WHERE p.id = :postId")
    Integer selectCommentCountById(@Param("postId") Long postId);

}
