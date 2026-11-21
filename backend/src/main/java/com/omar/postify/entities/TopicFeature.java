package com.omar.postify.entities;

import java.time.Instant;

public interface TopicFeature {

    Topic getTopic();

    long getImpressionCount();

    long getViewCount();

    long getViewStartCount();

    long getViewEndCount();

    long getLikeCount();

    long getSaveCount();

    long getCommentCount();

    long getTotalViewDurationMs();

    double getTotalCompletionRatio();

    long getCompletionRatioSamples();

    long getMediaStartCount();

    long getMediaProgressCount();

    long getMediaCompleteCount();

    long getMediaSkipCount();

    long getLinkClickCount();

    long getEventCount();

    Instant getLastInteractionAt();

    Instant getUpdatedAt();
}