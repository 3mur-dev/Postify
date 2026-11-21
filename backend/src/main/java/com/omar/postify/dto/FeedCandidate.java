package com.omar.postify.dto;

import com.omar.postify.entities.enums.CandidateSource;

public record FeedCandidate(
        Long postId,
        CandidateSource source,
        double relevanceScore,
        Long topicId
) {}