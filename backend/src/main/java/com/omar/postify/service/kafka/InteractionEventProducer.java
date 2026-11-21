package com.omar.postify.service.kafka;

import com.omar.postify.dto.InteractionEventMessage;
import com.omar.postify.dto.RequestIdentity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionEventProducer {

    private static final String TOPIC = "postify-interactions";

    private final KafkaTemplate<String, InteractionEventMessage> kafkaTemplate;

    public void publish(InteractionEventMessage event) {

        String eventId = event.clientEventId();

        if (eventId == null || eventId.isBlank()) {
            eventId = UUID.randomUUID().toString();

            event = new InteractionEventMessage(
                    event.identity(),
                    event.postId(),
                    event.eventType(),
                    event.occurredAt(),
                    event.sessionId(),
                    eventId,
                    event.positionMs(),
                    event.durationMs(),
                    event.completionRatio(),
                    event.source(),
                    event.metadata(),
                    event.eventVersion()
            );
        }

        /* eventId is now effectively final — safe to capture in the lambda. */
        final String finalEventId = eventId;

        RequestIdentity identity = event.identity();

        String key =
                identity.type().name()
                        + ":"
                        + identity.id();

        /*
         * Add callbacks so that Kafka send failures are logged instead of
         * silently lost. The fire-and-forget pattern means if Kafka is down,
         * the event disappears with no trace.
         */
        kafkaTemplate.send(TOPIC, key, event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to send interaction event to Kafka: eventId={}, error={}",
                                finalEventId, throwable.getMessage(), throwable);
                    } else {
                        RecordMetadata metadata = result.getRecordMetadata();
                        log.debug("Interaction event sent to Kafka: topic={}, partition={}, offset={}, eventId={}",
                                metadata.topic(), metadata.partition(), metadata.offset(), finalEventId);
                    }
                });
    }
}
