package com.omar.postify.service;

import com.omar.postify.dto.InteractionEventMessage;
import com.omar.postify.entities.AnonymousUser;
import com.omar.postify.entities.InteractionEvent;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.exception.ResourceNotFoundException;
import com.omar.postify.repository.AnonymousUserRepository;
import com.omar.postify.repository.InteractionEventRepository;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionEventService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final InteractionEventRepository interactionEventRepository;
    private final AnonymousUserRepository anonymousUserRepository;

    @Transactional
    public boolean saveEvent(InteractionEventMessage event) {

        if (interactionEventRepository.existsByClientEventId(
                event.clientEventId()
        )) {
            log.info("Ignoring duplicate interaction event: {}", event.clientEventId());
            return false;
        }

        try {
            Post post = postRepository.findById(event.postId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Post not in interaction"));

            InteractionEvent.InteractionEventBuilder builder = InteractionEvent.builder()
                    .post(post)
                    .eventType(event.eventType())
                    .occurredAt(event.occurredAt())
                    .sessionId(event.sessionId())
                    .clientEventId(event.clientEventId())
                    .positionMs(event.positionMs())
                    .durationMs(event.durationMs())
                    .completionRatio(event.completionRatio())
                    .source(event.source())
                    .metadata(event.metadata())
                    .eventVersion(event.eventVersion());

            if (event.identity().isAuthenticated()) {

                User user = userRepository.findById(event.identity().id())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found in interaction"));

                builder.user(user);

            } else {
                AnonymousUser anonymousUser = anonymousUserRepository.findById(event.identity().id())
                        .orElseThrow(() -> new ResourceNotFoundException("Anonymous user not found in interaction"));
                builder.anonymousUser(anonymousUser);
            }

            interactionEventRepository.save(builder.build());

            interactionEventRepository.flush();

        } catch (DataIntegrityViolationException e) {
            log.error(
                    "Failed to save interaction event: {}",
                    event.clientEventId(),
                    e
            );
            return false;
        }
        return true;
    }
}
