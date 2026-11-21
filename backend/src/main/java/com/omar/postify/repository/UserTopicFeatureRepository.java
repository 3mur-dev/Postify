package com.omar.postify.repository;

import com.omar.postify.entities.UserTopicFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface UserTopicFeatureRepository
        extends JpaRepository<UserTopicFeature, Long> {

    Optional<UserTopicFeature> findByUserIdAndTopicId(
            Long userId,
            Long topicId
    );

    @Query("""
            SELECT f
            FROM UserTopicFeature f
            JOIN FETCH f.topic
            WHERE f.user.id = :userId
            """)
    List<UserTopicFeature> findByUserId(@Param("userId") Long userId);
}
