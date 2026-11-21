package com.omar.postify.dto;

public record CandidateRelevance(
        double score,
        Long topicId
) {}