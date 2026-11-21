package com.omar.postify.service.feed;

import com.omar.postify.dto.CandidateGenerationContext;
import com.omar.postify.dto.FeedCandidate;
import com.omar.postify.entities.TopicFeature;
import com.omar.postify.entities.UserTopicFeature;
import com.omar.postify.entities.enums.CandidateSource;
import com.omar.postify.repository.AnonymousUserTopicFeatureRepository;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserTopicFeatureRepository;
import com.omar.postify.util.UserTopicInterestCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExplorationCandidateGenerator {

    private static final double STRONG_INTEREST_THRESHOLD = 0.30;

    private final PostRepository postRepository;
    private final UserTopicFeatureRepository userTopicFeatureRepository;
    private final UserTopicInterestCalculator interestCalculator;
    private final AnonymousUserTopicFeatureRepository anonymousUserTopicFeatureRepository;

    public List<FeedCandidate> generate(
            CandidateGenerationContext context,
            int limit
    ) {

        if (limit <= 0) {
            return List.of();
        }

        Set<Long> excludedPostIds = context.exclusions().excludedPostIds();

        Set<Long> excludedAuthorIds =
                context.followingUserIds();


        List<? extends TopicFeature> features;

        if (context.identity().isAnonymous()) {

            features = anonymousUserTopicFeatureRepository.findByAnonymousUserId(
                    context.identity().id()
            );

        } else {

            features = userTopicFeatureRepository.findByUserId(
                    context.identity().id()
            );
        }

        Set<Long> strongInterestTopicIds =
                features.stream()
                        .filter(feature ->
                                interestCalculator.calculate(feature)
                                        >= STRONG_INTEREST_THRESHOLD
                        )
                        .map(feature -> feature.getTopic().getId())
                        .collect(Collectors.toSet());

        List<Long> postIds;

        if (strongInterestTopicIds.isEmpty()) {

            postIds =
                    postRepository.findExplorationPostIdsWithoutTopics(
                            excludedAuthorIds,
                            excludedPostIds,
                            PageRequest.of(0, limit)
                    );

        } else {

            postIds =
                    postRepository.findExplorationPostIds(
                            excludedAuthorIds,
                            excludedPostIds,
                            strongInterestTopicIds,
                            PageRequest.of(0, limit)
                    );
        }

        return postIds.stream()
                .map(postId -> new FeedCandidate(
                        postId,
                        CandidateSource.EXPLORATION,
                        0,
                        null
                ))
                .toList();
    }
}