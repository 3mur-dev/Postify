package com.omar.postify.service;

import com.omar.postify.dto.InteractionEventMessage;
import com.omar.postify.dto.RequestIdentity;
import com.omar.postify.entities.AnonymousUser;
import com.omar.postify.entities.AnonymousUserTopicFeature;
import com.omar.postify.entities.PostTopic;
import com.omar.postify.repository.AnonymousUserRepository;
import com.omar.postify.repository.AnonymousUserTopicFeatureRepository;
import com.omar.postify.repository.PostTopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnonymousUserTopicFeatureService {

    private final AnonymousUserTopicFeatureRepository featureRepository;
    private final PostTopicRepository postTopicRepository;
    private final AnonymousUserRepository anonymousUserRepository;

    @Transactional
    public void processInteraction(
            RequestIdentity identity,
            Long postId,
            InteractionEventMessage event
    ) {

        AnonymousUser anonymousUser = anonymousUserRepository
                .findById(identity.id())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Anonymous user not found with ID: " + identity.id()
                ));

        List<PostTopic> postTopics =
                postTopicRepository.findByPostId(postId);

        for (PostTopic postTopic : postTopics) {

            AnonymousUserTopicFeature feature =
                    featureRepository
                            .findByAnonymousUserIdAndTopicId(
                                    anonymousUser.getId(),
                                    postTopic.getTopic().getId()
                            )
                            .orElseGet(() ->
                                    AnonymousUserTopicFeature.builder()
                                            .anonymousUser(anonymousUser)
                                            .topic(postTopic.getTopic())
                                            .build()
                            );

            applyEvent(feature, event);

            feature.setLastInteractionAt(event.occurredAt());

            featureRepository.save(feature);
        }
    }

    private void applyEvent(
            AnonymousUserTopicFeature feature,
            InteractionEventMessage event
    ) {
        feature.setEventCount(feature.getEventCount() + 1);

        switch (event.eventType()) {

            case IMPRESSION ->
                    feature.setImpressionCount(
                            feature.getImpressionCount() + 1
                    );

            case VIEW ->
                    feature.setViewCount(
                            feature.getViewCount() + 1
                    );

            case VIEW_START ->
                    feature.setViewStartCount(
                            feature.getViewStartCount() + 1
                    );

            case VIEW_END ->
                    feature.setViewEndCount(
                            feature.getViewEndCount() + 1
                    );

            case LIKE ->
                    feature.setLikeCount(
                            feature.getLikeCount() + 1
                    );

            case UNLIKE ->
                    feature.setLikeCount(
                            Math.max(0, feature.getLikeCount() - 1)
                    );

            case SAVE ->
                    feature.setSaveCount(
                            feature.getSaveCount() + 1
                    );

            case UNSAVE ->
                    feature.setSaveCount(
                            Math.max(0, feature.getSaveCount() - 1)
                    );

            case COMMENT ->
                    feature.setCommentCount(
                            feature.getCommentCount() + 1
                    );

            case MEDIA_START ->
                    feature.setMediaStartCount(
                            feature.getMediaStartCount() + 1
                    );

            case MEDIA_PROGRESS ->
                    feature.setMediaProgressCount(
                            feature.getMediaProgressCount() + 1
                    );

            case MEDIA_COMPLETE ->
                    feature.setMediaCompleteCount(
                            feature.getMediaCompleteCount() + 1
                    );

            case MEDIA_SKIP ->
                    feature.setMediaSkipCount(
                            feature.getMediaSkipCount() + 1
                    );

            case LINK_CLICK ->
                    feature.setLinkClickCount(
                            feature.getLinkClickCount() + 1
                    );

            default -> {
                // Event still contributes to eventCount.
            }
        }

        if (event.durationMs() != null) {
            feature.setTotalViewDurationMs(
                    feature.getTotalViewDurationMs()
                            + event.durationMs()
            );
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
