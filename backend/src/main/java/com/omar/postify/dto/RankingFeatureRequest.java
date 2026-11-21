package com.omar.postify.dto;

import java.util.List;

public record RankingFeatureRequest(
        List<RankingCandidate> candidates
) {
}