package com.omar.postify.repository;

import com.omar.postify.entities.PostVectorSync;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface PostVectorSyncRepository extends JpaRepository<PostVectorSync, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            insert into post_vector_sync (post_id, updated_at, revision)
            values (:postId, :updatedAt, 1)
            on duplicate key update
                updated_at = values(updated_at),
                revision = revision + 1
            """, nativeQuery = true)
    int enqueue(
            @Param("postId") Long postId,
            @Param("updatedAt") Instant updatedAt
    );

    List<PostVectorSync> findAllByOrderByUpdatedAtAsc(Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
            update PostVectorSync sync
            set sync.updatedAt = :updatedAt
            where sync.postId = :postId and sync.revision = :revision
            """)
    int markAttempted(
            @Param("postId") Long postId,
            @Param("revision") long revision,
            @Param("updatedAt") Instant updatedAt
    );

    @Modifying
    @Transactional
    @Query("delete from PostVectorSync sync where sync.postId = :postId and sync.revision = :revision")
    int deleteIfUnchanged(
            @Param("postId") Long postId,
            @Param("revision") long revision
    );
}
