package com.omar.postify.dto;

import java.util.List;

public record SearchResponse(
        List<UserSearchDto> users,
        List<PostFeedDto> posts
) {}