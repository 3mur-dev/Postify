package com.omar.postify.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RankingScore(
        @JsonProperty("post_id")
        Long postId,

        double score,

        int rank
) {
}