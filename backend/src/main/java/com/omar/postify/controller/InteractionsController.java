package com.omar.postify.controller;

import com.omar.postify.dto.ApiResponse;
import com.omar.postify.dto.InteractionEventMessage;
import com.omar.postify.dto.RequestIdentity;
import com.omar.postify.dto.TrackInteractionRequest;
import com.omar.postify.entities.Post;
import com.omar.postify.exception.ResourceNotFoundException;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.service.kafka.InteractionEventProducer;
import com.omar.postify.util.RequestIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interactions")
public class InteractionsController {

    private final InteractionEventProducer interactionEventProducer;
    private final RequestIdentityResolver requestIdentityResolver;
    private final PostRepository postRepository;

    @PostMapping
    public ApiResponse<?> postInteractionEvent(
            @RequestBody TrackInteractionRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {

        Post post = postRepository.findByPublicIdWithUserAndStatusIsActive(request.postPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        RequestIdentity identity =
                requestIdentityResolver.resolve(
                        httpRequest,
                        httpResponse
                );


        InteractionEventMessage event = new InteractionEventMessage(
                identity,
                post.getId(),
                request.eventType(),
                request.occurredAt() != null
                        ? request.occurredAt()
                        : Instant.now(),
                request.sessionId(),
                request.clientEventId(),
                request.positionMs(),
                request.durationMs(),
                request.completionRatio(),
                request.source(),
                request.metadata(),
                request.eventVersion()
        );

        interactionEventProducer.publish(event);

        return ApiResponse.success(
                "Interaction saved successfully",
                null
        );
    }
}
