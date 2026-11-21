package com.omar.postify.dto;

import java.util.Map;

public record RankingCandidate(
        Long postId,
        RankingFeatures features
) {
}