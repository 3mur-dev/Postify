package com.omar.postify.entities;

import java.time.Instant;

public interface MutableTopicFeature extends TopicFeature {

    void setEventCount(long value);

    void setImpressionCount(long value);

    void setViewCount(long value);

    void setViewStartCount(long value);

    void setViewEndCount(long value);

    void setLikeCount(long value);

    void setSaveCount(long value);

    void setCommentCount(long value);

    void setTotalViewDurationMs(long value);

    void setCompletionRatioSamples(long value);

    void setTotalCompletionRatio(double value);

    void setMediaStartCount(long value);

    void setMediaProgressCount(long value);

    void setMediaCompleteCount(long value);

    void setMediaSkipCount(long value);

    void setLinkClickCount(long value);

    void setLastInteractionAt(Instant value);
}