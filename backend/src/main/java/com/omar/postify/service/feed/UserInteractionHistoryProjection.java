package com.omar.postify.service.feed;

public interface UserInteractionHistoryProjection {

    long getImpressions();
    long getViews();
    long getLikes();
    long getSaves();
    long getComments();
    long getShares();

    long getTotalViewDurationMs();
    long getDurationSamples();
    double getTotalCompletionRatio();
    long getCompletionRatioSamples();
}