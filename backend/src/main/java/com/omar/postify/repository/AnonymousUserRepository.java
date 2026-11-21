package com.omar.postify.repository;

import com.omar.postify.entities.AnonymousUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnonymousUserRepository extends JpaRepository<AnonymousUser, Long> {

    Optional<AnonymousUser> findByPublicId(UUID publicId);

    @Modifying
    @Transactional
    @Query("update AnonymousUser user set user.lastSeenAt = :lastSeenAt where user.id = :id")
    int updateLastSeenAt(
            @Param("id") Long id,
            @Param("lastSeenAt") LocalDateTime lastSeenAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select user from AnonymousUser user
            where user.lastSeenAt < :cutoff
            order by user.id
            """)
    List<AnonymousUser> lockExpiredUsers(
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );
}