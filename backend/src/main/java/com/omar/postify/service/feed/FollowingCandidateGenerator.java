package com.omar.postify.service.feed;

import com.omar.postify.dto.CandidateGenerationContext;
import com.omar.postify.dto.FeedCandidate;
import com.omar.postify.entities.enums.CandidateSource;
import com.omar.postify.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FollowingCandidateGenerator {

    private final PostRepository postRepository;

    public List<FeedCandidate> generate(
            CandidateGenerationContext context,
            int limit
    ) {

        if (!context.identity().isAuthenticated() || limit <= 0) {
            return List.of();
        }

        Set<Long> followingUserIds =
                context.followingUserIds();

        if (followingUserIds.isEmpty()) {
            return List.of();
        }

        List<Long> postIds =
                postRepository.findPostIdsByUsers(
                        followingUserIds,
                        context.exclusions().excludedPostIds(),
                        PageRequest.of(0, limit)
                );

        return postIds.stream()
                .map(postId -> new FeedCandidate(
                        postId,
                        CandidateSource.FOLLOWING,
                        1.0,
                        null
                ))
                .toList();
    }
}
