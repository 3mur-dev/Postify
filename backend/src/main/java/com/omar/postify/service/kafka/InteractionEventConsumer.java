package com.omar.postify.service.kafka;

import com.omar.postify.dto.InteractionEventMessage;
import com.omar.postify.service.AnonymousUserTopicFeatureService;
import com.omar.postify.service.InteractionEventService;
import com.omar.postify.util.UserTopicInterestCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionEventConsumer {

    private final InteractionEventService interactionEventService;
    private final AnonymousUserTopicFeatureService anonymousUserTopicFeatureService;

    @KafkaListener(
            topics = "postify-interactions",
            groupId = "postify-interaction-consumers"
    )
    public void consume(InteractionEventMessage event) {

        boolean saved = interactionEventService.saveEvent(event);



        if (saved && !event.identity().isAuthenticated()) {
            anonymousUserTopicFeatureService.processInteraction(
                    event.identity(),
                    event.postId(),
                    event
            );
        }
    }
}