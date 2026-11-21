package com.omar.postify.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SavedPostResponse(

        LocalDateTime savedAt,

        PostFeedDto post

) {}
