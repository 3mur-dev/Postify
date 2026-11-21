package com.omar.postify.service;

import com.omar.postify.entities.User;
import com.omar.postify.repository.ConversationRepository;
import com.omar.postify.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PresenceTrackerTest {

    private final ConversationRepository conversationRepository =
            mock(ConversationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PresenceTracker tracker = new PresenceTracker(
            mock(org.springframework.messaging.simp.SimpMessagingTemplate.class),
            conversationRepository,
            userRepository
    );
    private final Principal user = () -> "john";

    @Test
    void staysOnlineUntilTheUsersLastConnectionCloses() {
        User persistedUser = new User();
        persistedUser.setId(7L);
        persistedUser.setUsername("john");
        persistedUser.setPublicId("john-public-id");
        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(persistedUser));
        when(conversationRepository.findConversationsByUser(7L))
                .thenReturn(java.util.List.of());

        tracker.onConnected(connected("session-1"));
        tracker.onConnected(connected("session-2"));
        assertTrue(tracker.isOnline("john"));

        tracker.onDisconnected(disconnected("session-1"));
        assertTrue(tracker.isOnline("john"));
        tracker.onDisconnected(disconnected("session-2"));
        assertFalse(tracker.isOnline("john"));

        verify(userRepository, org.mockito.Mockito.atLeastOnce())
                .findByUsername("john");
    }

    private SessionConnectedEvent connected(String sessionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(
                StompCommand.CONNECTED
        );
        accessor.setSessionId(sessionId);
        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionConnectedEvent(this, message, user);
    }

    private SessionDisconnectEvent disconnected(String sessionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(
                StompCommand.DISCONNECT
        );
        accessor.setSessionId(sessionId);
        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionDisconnectEvent(
                this,
                message,
                sessionId,
                CloseStatus.NORMAL,
                user
        );
    }
}
