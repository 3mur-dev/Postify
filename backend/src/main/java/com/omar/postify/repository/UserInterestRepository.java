package com.omar.postify.repository;

import com.omar.postify.entities.UserInterest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserInterestRepository
        extends JpaRepository<UserInterest, Long> {

    @Query("""
    SELECT ui.topic.id
    FROM UserInterest ui
    WHERE ui.user.id = :userId
    ORDER BY ui.score DESC
""")
    List<Long> findTopTopicIds(
            @Param("userId") Long userId,
            Pageable pageable
    );
}