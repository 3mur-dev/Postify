package com.omar.postify.dto;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public record CandidateGenerationContext(
        RequestIdentity identity,
        int candidateLimit,
        Set<Long> followingUserIds,
        FeedExclusionContext exclusions,
        Instant sessionStartedAt
) {}