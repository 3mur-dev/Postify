package com.omar.postify.dto;

import java.util.HashSet;
import java.util.Set;

public record FeedExclusionContext(
        Set<Long> seenPostIds,
        Set<Long> userPostIds

) {

    public boolean containsPost(Long postId) {

        return seenPostIds.contains(postId) || userPostIds.contains(postId);
    }

    public Set<Long> excludedPostIds() {

        Set<Long> excluded = new HashSet<>(seenPostIds);

        excluded.addAll(userPostIds);

        return excluded;
    }

}
