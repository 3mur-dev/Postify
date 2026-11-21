package com.omar.postify.service.kafka;

import com.omar.postify.dto.InteractionEventMessage;
import com.omar.postify.service.UserInterestFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInterestConsumer {

    private final UserInterestFeatureService featureService;

    @KafkaListener(
            topics = "postify-interactions",
            groupId = "postify-user-interest"
    )
    public void consume(InteractionEventMessage event) {
        featureService.process(event);
    }
}
