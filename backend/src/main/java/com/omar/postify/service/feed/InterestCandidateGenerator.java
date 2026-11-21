package com.omar.postify.service.feed;

import com.omar.postify.dto.CandidateGenerationContext;
import com.omar.postify.dto.CandidateRelevance;
import com.omar.postify.dto.FeedCandidate;
import com.omar.postify.entities.PostTopic;
import com.omar.postify.entities.TopicFeature;
import com.omar.postify.entities.UserTopicFeature;
import com.omar.postify.entities.enums.CandidateSource;
import com.omar.postify.repository.AnonymousUserTopicFeatureRepository;
import com.omar.postify.repository.PostTopicRepository;
import com.omar.postify.repository.UserTopicFeatureRepository;
import com.omar.postify.util.UserTopicInterestCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class InterestCandidateGenerator {

    private final PostTopicRepository postTopicRepository;
    private final UserTopicFeatureRepository userTopicFeatureRepository;
    private final UserTopicInterestCalculator interestCalculator;
    private final AnonymousUserTopicFeatureRepository anonymousUserTopicFeatureRepository;

    public List<FeedCandidate> generate(CandidateGenerationContext context, int limit) {

        if (limit <= 0) {
            return List.of();
        }

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

        Map<Long, Double> topicInterests =
                features.stream()
                        .map(feature -> Map.entry(
                                feature.getTopic().getId(),
                                interestCalculator.calculate(feature)
                        ))
                        .filter(entry -> entry.getValue() >= 0.05)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        ));

        if (topicInterests.isEmpty()) {
            return List.of();
        }

        log.info(
                "Identity={} topicInterests={}",
                context.identity(),
                topicInterests
        );

        List<PostTopic> postTopics =
                postTopicRepository.findByTopicIdIn(
                        topicInterests.keySet()
                );

        Map<Long, CandidateRelevance> relevanceByPost =
                postTopics.stream()
                        .filter(postTopic -> !context.exclusions()
                                .containsPost(
                                        postTopic.getPost().getId()
                                )
                        )

                        .collect(Collectors.groupingBy(
                                postTopic -> postTopic.getPost().getId(),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        topics -> topics.stream()
                                                .map(postTopic -> {
                                                    Long topicId =
                                                            postTopic.getTopic().getId();

                                                    double interest =
                                                            topicInterests.getOrDefault(
                                                                    topicId,
                                                                    0.0
                                                            );

                                                    double relevance =
                                                            interest
                                                                    * postTopic.getConfidence();

                                                    return new CandidateRelevance(
                                                            relevance,
                                                            topicId
                                                    );
                                                })
                                                .max(
                                                        Comparator.comparingDouble(
                                                                CandidateRelevance::score
                                                        )
                                                )
                                                .orElse(
                                                        new CandidateRelevance(0.0, null)
                                                )
                                )
                        ));

        return relevanceByPost.entrySet()
                .stream()
                .sorted(
                        Map.Entry.<Long, CandidateRelevance>comparingByValue(
                                Comparator.comparingDouble(
                                        CandidateRelevance::score
                                )
                        ).reversed()
                )
                .limit(limit)
                .map(entry -> new FeedCandidate(
                        entry.getKey(),
                        CandidateSource.INTEREST,
                        entry.getValue().score(),
                        entry.getValue().topicId()
                ))
                .toList();
    }
}
