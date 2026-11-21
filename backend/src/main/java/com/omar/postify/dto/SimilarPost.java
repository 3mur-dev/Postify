package com.omar.postify.dto;

public record SimilarPost(
        Long postId,
        float score
) {}