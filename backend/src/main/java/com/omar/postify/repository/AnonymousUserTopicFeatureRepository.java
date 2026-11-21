package com.omar.postify.repository;

import com.omar.postify.entities.AnonymousUserTopicFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnonymousUserTopicFeatureRepository
        extends JpaRepository<AnonymousUserTopicFeature, Long> {

    List<AnonymousUserTopicFeature> findByAnonymousUserId(Long anonymousUserId);

    @Modifying
    @Query("delete from AnonymousUserTopicFeature feature where feature.anonymousUser.id in :anonymousUserIds")
    int deleteByAnonymousUserIds(@Param("anonymousUserIds") List<Long> anonymousUserIds);

    Optional<AnonymousUserTopicFeature> findByAnonymousUserIdAndTopicId(
            Long anonymousUserId,
            Long topicId
    );
}