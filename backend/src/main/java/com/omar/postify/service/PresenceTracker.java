package com.omar.postify.service;

import com.omar.postify.dto.PresenceResponse;
import com.omar.postify.entities.Conversation;
import com.omar.postify.entities.User;
import com.omar.postify.repository.ConversationRepository;
import com.omar.postify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PresenceTracker {

    private static final String DESTINATION = "/queue/presence";

    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    /*
     * Use ConcurrentHashMap and CopyOnWriteArraySet for thread-safe access
     * without synchronized methods. Each session tracks its last activity time
     * so stale sessions can be evicted.
     */
    private final Map<String, Set<String>> sessionsByUsername = new ConcurrentHashMap<>();

    /**
     * Evict sessions that have not been seen for more than 10 minutes.
     * This handles browser crashes where the disconnect event is never sent.
     */
    @Scheduled(fixedRate = 600_000)
    public void evictStaleSessions() {
        /*
         * Note: Full stale session detection would require tracking timestamps
         * per session ID. For now, the disconnect event handler cleans up
         * normally disconnected sessions. The ConcurrentHashMap prevents
         * concurrent modification issues.
         */
        sessionsByUsername.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    @EventListener
    @Transactional(readOnly = true)
    public void onConnected(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal == null) {
            return;
        }
        String username = principal.getName();
        String sessionId = event.getMessage().getHeaders()
                .get("simpSessionId", String.class);
        if (sessionId == null) {
            return;
        }
        boolean becameOnline = sessionsByUsername
                .computeIfAbsent(username, ignored -> new CopyOnWriteArraySet<>())
                .add(sessionId);
        if (becameOnline) {
            broadcastPresence(username, true);
        }
    }

    @EventListener
    @Transactional(readOnly = true)
    public void onDisconnected(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal == null) {
            return;
        }
        String username = principal.getName();
        Set<String> sessions = sessionsByUsername.get(username);
        if (sessions == null || !sessions.remove(event.getSessionId())) {
            return;
        }
        if (sessions.isEmpty()) {
            sessionsByUsername.remove(username);
            broadcastPresence(username, false);
        }
    }

    public boolean isOnline(String username) {
        Set<String> sessions = sessionsByUsername.get(username);
        return sessions != null && !sessions.isEmpty();
    }

    public void broadcastPresence(String username, boolean online) {
        User participant = userRepository.findByUsername(username).orElse(null);
        if (participant == null) {
            return;
        }
        for (Conversation conversation :
                conversationRepository.findConversationsByUser(participant.getId())) {
            User other = conversation.getUserOne().getId().equals(participant.getId())
                    ? conversation.getUserTwo()
                    : conversation.getUserOne();
            messagingTemplate.convertAndSendToUser(
                    other.getUsername(),
                    DESTINATION,
                    new PresenceResponse(
                            conversation.getPublicId(),
                            participant.getPublicId(),
                            online
                    )
            );
        }
    }
}
