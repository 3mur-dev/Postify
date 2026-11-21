package com.omar.postify.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record RankingRequest(

        @JsonProperty("candidates")
        List<RankingFeatures> candidates,

        @JsonProperty("session_started_at")
        Instant sessionStartedAt
) {}