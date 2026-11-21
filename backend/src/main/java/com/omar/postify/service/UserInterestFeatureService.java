package com.omar.postify.service;

import com.omar.postify.dto.InteractionEventMessage;
import com.omar.postify.entities.*;
import com.omar.postify.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserInterestFeatureService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserTopicFeatureRepository featureRepository;
    private final PostTopicRepository postTopicRepository;
    private final AnonymousUserRepository anonymousUserRepository;
    private final AnonymousUserTopicFeatureRepository anonymousFeatureRepository;

    @Transactional
    public void process(InteractionEventMessage event) {

        Post post = postRepository.findById(event.postId())
                .orElseThrow();

        List<PostTopic> postTopics =
                postTopicRepository.findAllByPost(post);

        if (event.identity().isAuthenticated()) {

            User user = userRepository.findById(
                            event.identity().id()
                    )
                    .orElseThrow();

            for (PostTopic postTopic : postTopics) {

                Topic topic = postTopic.getTopic();

                UserTopicFeature feature =
                        featureRepository.findByUserIdAndTopicId(
                                        user.getId(),
                                        topic.getId()
                                )
                                .orElseGet(() ->
                                        UserTopicFeature.builder()
                                                .user(user)
                                                .topic(topic)
                                                .build()
                                );

                applyEvent(feature, event);

                feature.setLastInteractionAt(
                        event.occurredAt()
                );

                featureRepository.save(feature);
            }

        } else {

            AnonymousUser anonymousUser =
                    anonymousUserRepository.findById(
                                    event.identity().id()
                            )
                            .orElseThrow();

            for (PostTopic postTopic : postTopics) {

                Topic topic = postTopic.getTopic();

                AnonymousUserTopicFeature feature =
                        anonymousFeatureRepository
                                .findByAnonymousUserIdAndTopicId(
                                        anonymousUser.getId(),
                                        topic.getId()
                                )
                                .orElseGet(() ->
                                        AnonymousUserTopicFeature.builder()
                                                .anonymousUser(anonymousUser)
                                                .topic(topic)
                                                .build()
                                );

                applyEvent(feature, event);

                feature.setLastInteractionAt(
                        event.occurredAt()
                );

                anonymousFeatureRepository.save(feature);
            }
        }
    }

    private void applyEvent(
            MutableTopicFeature feature,
            InteractionEventMessage event
    ) {

        feature.setEventCount(
                feature.getEventCount() + 1
        );

        switch (event.eventType()) {

            case IMPRESSION -> feature.setImpressionCount(
                    feature.getImpressionCount() + 1
            );

            case VIEW -> feature.setViewCount(
                    feature.getViewCount() + 1
            );

            case VIEW_START -> feature.setViewStartCount(
                    feature.getViewStartCount() + 1
            );

            case VIEW_END -> {
                feature.setViewEndCount(
                        feature.getViewEndCount() + 1
                );

                if (event.durationMs() != null) {
                    feature.setTotalViewDurationMs(
                            feature.getTotalViewDurationMs()
                                    + event.durationMs()
                    );
                }
            }

            case LIKE -> feature.setLikeCount(
                    feature.getLikeCount() + 1
            );

            case SAVE -> feature.setSaveCount(
                    feature.getSaveCount() + 1
            );

            case COMMENT -> feature.setCommentCount(
                    feature.getCommentCount() + 1
            );

            case MEDIA_START -> feature.setMediaStartCount(
                    feature.getMediaStartCount() + 1
            );

            case MEDIA_PROGRESS -> feature.setMediaProgressCount(
                    feature.getMediaProgressCount() + 1
            );

            case MEDIA_COMPLETE -> feature.setMediaCompleteCount(
                    feature.getMediaCompleteCount() + 1
            );

            case MEDIA_SKIP -> feature.setMediaSkipCount(
                    feature.getMediaSkipCount() + 1
            );

            case LINK_CLICK -> feature.setLinkClickCount(
                    feature.getLinkClickCount() + 1
            );

            default -> {
            }
        }

        if (event.completionRatio() != null) {

            feature.setTotalCompletionRatio(
                    feature.getTotalCompletionRatio()
                            + event.completionRatio()
            );

            feature.setCompletionRatioSamples(
                    feature.getCompletionRatioSamples() + 1
            );
        }
    }
}