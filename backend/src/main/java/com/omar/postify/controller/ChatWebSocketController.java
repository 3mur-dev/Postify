package com.omar.postify.controller;

import com.omar.postify.dto.ChatMessageRequest;
import com.omar.postify.dto.MessageResponse;
import com.omar.postify.dto.PresenceRequest;
import com.omar.postify.dto.PresenceResponse;
import com.omar.postify.dto.SendMessageRequest;
import com.omar.postify.service.ConversationService;
import com.omar.postify.service.MessageService;
import com.omar.postify.service.PresenceTracker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@RequiredArgsConstructor
@Controller
@Validated
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ConversationService conversationService;
    private final PresenceTracker presenceTracker;

    @MessageMapping("/chat.send")
    public void chat(
            Principal principal,
            @Valid ChatMessageRequest chatMessageRequest
    ) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (chatMessageRequest == null) {
            throw new IllegalArgumentException("Message request is required");
        }
        SendMessageRequest sendMessageRequest = new SendMessageRequest(chatMessageRequest.content());

        String recipientUsername = conversationService.getOtherParticipantUsername(
                chatMessageRequest.conversationPublicId(),
                principal.getName()
        );

        MessageResponse messageResponse = messageService.sendMessage(
                chatMessageRequest.conversationPublicId(),
                sendMessageRequest,
                principal.getName(),
                chatMessageRequest.clientMessageId()
        );

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/messages",
                messageResponse
        );

        messagingTemplate.convertAndSendToUser(
                recipientUsername,
                "/queue/messages",
                messageResponse
        );
    }

    @MessageMapping("/chat.presence")
    public void getParticipantPresence(
            Principal principal,
            @Valid PresenceRequest request
    ) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Presence request is required");
        }

        String participantPublicId = conversationService.getOtherParticipantPublicId(
                request.conversationPublicId(),
                principal.getName()
        );
        String participantUsername = conversationService.getOtherParticipantUsername(
                request.conversationPublicId(),
                principal.getName()
        );
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/presence",
                new PresenceResponse(
                        request.conversationPublicId(),
                        participantPublicId,
                        presenceTracker.isOnline(participantUsername)
                )
        );
    }
}
