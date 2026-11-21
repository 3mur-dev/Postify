package com.omar.postify.repository;

public interface TopicScoreProjection {
    Long getTopicId();
    long getViewCount();
    long getLikeCount();
    long getSaveCount();
}