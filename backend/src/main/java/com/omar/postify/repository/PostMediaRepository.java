package com.omar.postify.repository;

import com.omar.postify.dto.PostMediaDto;
import com.omar.postify.dto.PostMediaProjection;
import com.omar.postify.entities.PostMedia;
import com.omar.postify.entities.enums.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {

    @Query("""
    SELECT new com.omar.postify.dto.PostMediaProjection(
        pm.post.id,
        pm.id,
        pm.url,
        pm.type,
        pm.position,
        pm.width,
        pm.height,
        pm.durationMs
    )
    FROM PostMedia pm
    WHERE pm.post.id IN :postIds
    ORDER BY pm.post.id, pm.position
    """)
    List<PostMediaProjection> findMediaByPostIds(
            @Param("postIds") Collection<Long> postIds
    );

    List<PostMedia> findByPostIdOrderByPositionAsc(Long postId);

    void deleteByPostId(Long postId);

    @Query("""
            SELECT pm FROM PostMedia pm
            LEFT JOIN FETCH pm.post
            WHERE pm.id = :mediaId
            AND pm.post.status = :status
            """)
    Optional<PostMedia> findByIdAndPostStatus(
            @Param("mediaId") Long mediaId,
            @Param("status") PostStatus status
    );
}