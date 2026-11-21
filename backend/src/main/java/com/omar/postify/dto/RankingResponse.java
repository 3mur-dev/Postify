package com.omar.postify.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RankingResponse(
        @JsonProperty("model_version")
        String modelVersion,

        @JsonProperty("candidate_count")
        int candidateCount,

        @JsonProperty("ranked_candidates")
        List<RankingScore> rankedCandidates
) {}
